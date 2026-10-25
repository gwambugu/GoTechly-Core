package com.dotechly.gotechly.core.config.cluster;

import com.dotechly.gotechly.core.config.PluginLoader;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.topic.ITopic;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Cross-instance fan-out of plugin enable/disable actions.
 * <p>
 * When an operator disables (or enables) a plugin from the admin portal, the request lands on a
 * single Core instance. Without coordination only that instance would unload/reload the JAR while
 * every other member kept serving the plugin. This manager makes the action cluster-wide, mirroring
 * {@link ClusterConfigRefresher}:
 * <ol>
 *   <li>The origin applies the change locally via {@link PluginLoader} (unload on disable, reload on
 *       enable).</li>
 *   <li>It records the <em>desired state</em> in a Hazelcast {@link IMap}
 *       ({@code paymentType -> enabled?}). This is the source of truth a late-joining or restarted
 *       member converges to.</li>
 *   <li>It publishes a message on a reliable {@link ITopic}; every peer's listener applies the
 *       change immediately.</li>
 * </ol>
 * The state {@link IMap} is the self-heal layer: {@link #reconcile()} (and the {@link #start()}
 * baseline sweep) re-apply the desired state on any member whose local plugin registry has drifted
 * — e.g. a member that was down when the topic message fired, or one that just (re)loaded every JAR
 * at boot and must now honour an existing disable.
 * <p>
 * <b>Restart scope:</b> the desired state lives in Hazelcast (backed up), so it survives individual
 * member restarts and rolling restarts. A <em>full</em> cluster shutdown loses it (every member
 * reloads all JARs enabled) — the same durability profile as {@link ClusterConfigRefresher}'s epoch
 * map. Persisting the desired state to {@code app_configs} and skipping disabled JARs at boot would
 * close that gap; it is intentionally out of scope here.
 *
 * @see PluginLoader#disablePlugin(String)
 * @see PluginLoader#enablePlugin(String)
 */
@Component
public class ClusterPluginStateManager {

    static final String TOPIC = "plugin-state-change";
    static final String STATE_MAP = "plugin-enabled-state";

    private static final Logger LOG = LoggerFactory.getLogger(ClusterPluginStateManager.class);

    private final HazelcastInstance hazelcast;
    private final PluginLoader pluginLoader;

    /** Desired state per payment type: {@code TRUE} = enabled, {@code FALSE} = disabled. */
    private IMap<String, Boolean> desiredState;
    private ITopic<PluginStateMessage> topic;
    private String localUuid;

    public ClusterPluginStateManager(HazelcastInstance hazelcast, PluginLoader pluginLoader) {
        this.hazelcast = hazelcast;
        this.pluginLoader = pluginLoader;
    }

    @PostConstruct
    void start() {
        this.desiredState = hazelcast.getMap(STATE_MAP);
        this.topic = hazelcast.getReliableTopic(TOPIC);
        this.localUuid = hazelcast.getCluster().getLocalMember().getUuid().toString();

        topic.addMessageListener(msg -> {
            PluginStateMessage m = msg.getMessageObject();
            if (m.originUuid() != null && m.originUuid().equals(localUuid)) {
                return;   // our own broadcast echoed back — we already applied before publishing
            }
            applyLocal(m.paymentType(), m.enabled());
        });

        // Baseline: this member just (re)loaded every plugin from disk at startup, all enabled.
        // Converge to any disable that was already in effect cluster-wide before we joined.
        reconcile();
        LOG.info("ClusterPluginStateManager active (member={}, knownStates={})", localUuid, desiredState.size());
    }

    /**
     * Disable {@code paymentType} on every member. Applies locally first (so the API response
     * reflects a real outcome), then records the desired state and broadcasts.
     *
     * @return {@code true} if the plugin is now disabled on this instance; {@code false} if there is
     *         no JAR to disable (e.g. a config-only composite or unknown payment type)
     */
    public boolean disableClusterWide(String paymentType) {
        if (paymentType == null || paymentType.isBlank()) {
            return false;
        }
        // disablePlugin returns false when already disabled OR when there is no JAR; the disabled-set
        // check distinguishes "already disabled" (success, idempotent) from "nothing to disable".
        boolean localOk = pluginLoader.disablePlugin(paymentType)
                || pluginLoader.getDisabledPaymentTypes().contains(paymentType);
        if (!localOk) {
            return false;
        }
        broadcast(paymentType, false);
        return true;
    }

    /**
     * Enable {@code paymentType} on every member.
     *
     * @return {@code true} if the plugin is enabled on this instance; {@code false} if it was
     *         disabled and could not be reloaded (e.g. its JAR is no longer on disk)
     */
    public boolean enableClusterWide(String paymentType) {
        if (paymentType == null || paymentType.isBlank()) {
            return false;
        }
        boolean wasDisabled = pluginLoader.getDisabledPaymentTypes().contains(paymentType);
        boolean localOk = !wasDisabled || pluginLoader.enablePlugin(paymentType);
        if (!localOk) {
            return false;   // was disabled but reload failed (JAR missing) — leave cluster state alone
        }
        broadcast(paymentType, true);
        return true;
    }

    private void broadcast(String paymentType, boolean enabled) {
        try {
            desiredState.put(paymentType, enabled);
            topic.publish(new PluginStateMessage(paymentType, enabled, localUuid));
            LOG.info("Broadcast plugin {} for '{}' to cluster", enabled ? "enable" : "disable", paymentType);
        } catch (Exception e) {
            LOG.warn("Failed to broadcast plugin state for '{}' ({}); peers will catch up on next " +
                    "reconcile.", paymentType, e.getMessage());
        }
    }

    /**
     * Self-heal sweep: re-apply the cluster's desired state locally. Catches a member that missed a
     * topic message (was down / partitioned) or that just reloaded its JARs at boot. Runs on every
     * member — deliberately NOT a ShedLock singleton (each instance must converge its own in-memory
     * plugin registry), matching {@link ClusterConfigRefresher#reconcile()}.
     */
    @Scheduled(fixedDelayString = "${gotechly.hazelcast.reconcile-ms:60000}")
    void reconcile() {
        if (desiredState == null) {
            return;
        }
        try {
            desiredState.forEach(this::applyLocal);
        } catch (Exception e) {
            LOG.warn("Plugin state reconcile sweep failed: {}", e.getMessage());
        }
    }

    /**
     * Apply a desired state to this member. Both {@link PluginLoader} calls are idempotent and
     * {@code synchronized}, so a redundant apply (already in the target state) is a cheap no-op.
     */
    private void applyLocal(String paymentType, boolean enabled) {
        try {
            if (enabled) {
                pluginLoader.enablePlugin(paymentType);
            } else {
                pluginLoader.disablePlugin(paymentType);
            }
        } catch (Exception e) {
            LOG.error("Local plugin {} for '{}' failed: {}",
                    enabled ? "enable" : "disable", paymentType, e.getMessage(), e);
        }
    }

    /** Topic payload. {@code originUuid} lets the publishing member skip its own echoed message. */
    public record PluginStateMessage(String paymentType, boolean enabled, String originUuid)
            implements Serializable {}
}
