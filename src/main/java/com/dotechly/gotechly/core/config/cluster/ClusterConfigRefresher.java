package com.dotechly.gotechly.core.config.cluster;


import com.dotechly.gotechly.core.config.PluginLoader;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.topic.ITopic;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cross-instance fan-out of plugin config refreshes.
 * <p>
 * When an {@code app_configs} / {@code error_codes} change is approved, the processing instance
 * calls {@link #refreshClusterWide(String)} which (1) refreshes its own loaded plugins via
 * {@link PluginLoader} immediately, (2) bumps a cluster-wide epoch, and (3) publishes a message on
 * a Hazelcast {@link ITopic}. Every other member's listener picks it up and refreshes locally.
 * <p>
 * The epoch {@link IMap} is the self-heal layer: a member that was down or partitioned when the
 * topic message fired catches up in {@link #reconcile()} (or on first start) by comparing the
 * cluster epochs against what it has locally applied. Push gives instant propagation; the epoch
 * reconcile guarantees eventual convergence.
 *
 * @see PluginLoader#refreshPluginConfig(String)
 */
@Component
public class ClusterConfigRefresher {

    /** Sentinel payment type meaning "all loaded plugins" — used for {@code core.*} globals. */
    public static final String ALL = "__ALL__";

    static final String TOPIC = "plugin-config-refresh";
    static final String EPOCH_MAP = "plugin-config-epoch";

    private static final Logger LOG = LoggerFactory.getLogger(ClusterConfigRefresher.class);

    private final HazelcastInstance hazelcast;
    private final PluginLoader pluginLoader;

    private ITopic<ConfigRefreshMessage> topic;
    private IMap<String, Long> epochs;
    /** Highest epoch this member has already applied, per payment type. */
    private final Map<String, Long> applied = new ConcurrentHashMap<>();
    private String localUuid;

    public ClusterConfigRefresher(HazelcastInstance hazelcast, PluginLoader pluginLoader) {
        this.hazelcast = hazelcast;
        this.pluginLoader = pluginLoader;
    }

    @PostConstruct
    void start() {
        this.topic = hazelcast.getReliableTopic(TOPIC);
        this.epochs = hazelcast.getMap(EPOCH_MAP);
        this.localUuid = hazelcast.getCluster().getLocalMember().getUuid().toString();

        // Baseline: this member just loaded every plugin from current DB state at startup, so mark
        // all known epochs as already-applied. Without this, the first reconcile() would needlessly
        // re-init every plugin.
        epochs.forEach(applied::put);

        topic.addMessageListener(msg -> {
            ConfigRefreshMessage m = msg.getMessageObject();
            applyIfNewer(m.paymentType(), m.epoch(), m.originUuid());
        });
        LOG.info("ClusterConfigRefresher active (member={}, knownEpochs={})", localUuid, applied.size());
    }

    /**
     * Refresh this member now, then broadcast to peers. Called from the approval after-commit hook.
     *
     * @param paymentType the affected payment type, or {@link #ALL} for a cross-plugin global
     */
    public void refreshClusterWide(String paymentType) {
        if (paymentType == null || paymentType.isBlank()) {
            return;
        }
        long epoch;
        try {
            epoch = epochs.executeOnKey(paymentType, new IncrementProcessor());
        } catch (Exception e) {
            // Hazelcast unavailable (e.g. mid-restart) — still refresh locally so a single-instance
            // or degraded deployment is not left stale.
            LOG.warn("Epoch bump failed for '{}' ({}); refreshing locally only.", paymentType, e.getMessage());
            doLocalRefresh(paymentType);
            return;
        }
        applyIfNewer(paymentType, epoch, null);          // refresh THIS member (origin null = apply)
        try {
            topic.publish(new ConfigRefreshMessage(paymentType, localUuid, epoch));   // tell peers
        } catch (Exception e) {
            LOG.warn("Failed to publish config-refresh for '{}' to cluster ({}); peers will catch up " +
                    "on next reconcile.", paymentType, e.getMessage());
        }
    }

    /** Applies a refresh only if {@code epoch} is newer than what this member already applied. */
    private synchronized void applyIfNewer(String paymentType, long epoch, String originUuid) {
        if (originUuid != null && originUuid.equals(localUuid)) {
            return;   // our own broadcast echoed back — we already refreshed before publishing
        }
        Long current = applied.get(paymentType);
        if (current != null && current >= epoch) {
            return;   // idempotent — already applied this or a newer epoch
        }
        doLocalRefresh(paymentType);
        applied.put(paymentType, epoch);
    }

    private void doLocalRefresh(String paymentType) {
        try {
            if (ALL.equals(paymentType)) {
                pluginLoader.refreshAllPluginConfigs();
            } else {
                pluginLoader.refreshPluginConfig(paymentType);
            }
        } catch (Exception e) {
            LOG.error("Local plugin refresh for '{}' failed: {}", paymentType, e.getMessage(), e);
        }
    }

    /**
     * Self-heal sweep: re-apply any cluster epoch newer than this member's applied epoch. Catches a
     * member that missed a topic message (was down / partitioned). Runs on every member — this is
     * deliberately NOT a ShedLock singleton job (unlike the EOD jobs); each instance must reconcile
     * its own in-memory plugin state.
     */
    @Scheduled(fixedDelayString = "${gotechly.hazelcast.reconcile-ms:60000}")
    void reconcile() {
        try {
            epochs.forEach((paymentType, epoch) -> applyIfNewer(paymentType, epoch, null));
        } catch (Exception e) {
            LOG.warn("Config epoch reconcile sweep failed: {}", e.getMessage());
        }
    }

    /** Topic payload. {@code originUuid} lets the publishing member skip its own echoed message. */
    public record ConfigRefreshMessage(String paymentType, String originUuid, long epoch)
            implements Serializable {}

    /** Atomic, cluster-wide monotonic increment executed on the IMap partition owner. */
    static final class IncrementProcessor
            implements EntryProcessor<String, Long, Long>, Serializable {
        @Override
        public Long process(Map.Entry<String, Long> entry) {
            long next = (entry.getValue() == null ? 0L : entry.getValue()) + 1L;
            entry.setValue(next);
            return next;
        }
    }
}
