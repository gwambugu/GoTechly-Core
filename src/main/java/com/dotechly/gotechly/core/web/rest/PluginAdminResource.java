package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.config.PluginInfo;
import com.dotechly.gotechly.core.config.PluginLoader;
import com.dotechly.gotechly.core.config.cluster.ClusterPluginStateManager;
import com.dotechly.gotechly.core.config.plugin.PluginVerifier;
import com.dotechly.gotechly.core.config.plugin.VerificationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin")
public class PluginAdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(PluginAdminResource.class);

    private final PluginLoader pluginLoader;
    private final PluginVerifier pluginVerifier;
    private final ClusterPluginStateManager clusterPluginStateManager;

    public PluginAdminResource(PluginLoader pluginLoader, PluginVerifier pluginVerifier,
                               ClusterPluginStateManager clusterPluginStateManager) {
        this.pluginLoader = pluginLoader;
        this.pluginVerifier = pluginVerifier;
        this.clusterPluginStateManager = clusterPluginStateManager;
    }

    public record VerificationStatusResponse(
            PluginVerifier.VerifierSummary summary,
            Collection<VerificationRecord> files
    ) {
    }

    /**
     * GET /api/v1/admin/plugins
     * Returns metadata about all currently loaded plugins.
     */
    @GetMapping("/plugins")
    public ResponseEntity<List<PluginInfo>> getLoadedPlugins() {
        LOG.debug("REST request to get loaded plugins info");
        List<PluginInfo> plugins = pluginLoader.getLoadedPluginInfos();
        LOG.info("Loaded {} plugins", plugins.size());
        return ResponseEntity.ok(plugins);
    }

    /**
     * POST /api/v1/admin/plugins/{paymentType}/disable
     * Disables all handlers for the given payment type across every Core instance in the cluster.
     */
    @PostMapping("/plugins/{paymentType}/disable")
    public ResponseEntity<Void> disablePlugin(@PathVariable String paymentType) {
        LOG.info("REST request to disable plugin cluster-wide: {}", paymentType);
        boolean disabled = clusterPluginStateManager.disableClusterWide(paymentType);
        if (disabled) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * POST /api/v1/admin/plugins/{paymentType}/enable
     * Re-enables a previously disabled plugin across every Core instance in the cluster.
     */
    @PostMapping("/plugins/{paymentType}/enable")
    public ResponseEntity<Void> enablePlugin(@PathVariable String paymentType) {
        LOG.info("REST request to enable plugin cluster-wide: {}", paymentType);
        boolean enabled = clusterPluginStateManager.enableClusterWide(paymentType);
        if (enabled) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * GET /api/v1/admin/plugins/disabled
     * Returns the set of payment types that are currently disabled.
     */
    @GetMapping("/plugins/disabled")
    public ResponseEntity<Set<String>> getDisabledPlugins() {
        LOG.debug("REST request to get disabled plugins");
        return ResponseEntity.ok(pluginLoader.getDisabledPaymentTypes());
    }

    /**
     * GET /api/v1/admin/plugins/verification-status
     * Returns signature-verification summary and per-file records
     * ({@code VERIFIED}, {@code REJECTED}, {@code SKIPPED}).
     * Optional {@code ?status=} filter narrows to one status.
     */
    @GetMapping("/plugins/verification-status")
    public ResponseEntity<VerificationStatusResponse> getVerificationStatus(
            @RequestParam(value = "status", required = false) String statusFilter) {
        LOG.debug("REST request for plugin verification status (filter={})", statusFilter);

        Collection<VerificationRecord> records = pluginVerifier.allRecords();
        if (statusFilter != null && !statusFilter.isBlank()) {
            VerificationRecord.Status target;
            try {
                target = VerificationRecord.Status.valueOf(statusFilter.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
            records = records.stream().filter(r -> r.status() == target).toList();
        }
        return ResponseEntity.ok(new VerificationStatusResponse(pluginVerifier.summary(), records));
    }

    /**
     * POST /api/v1/admin/plugins/verification-status/rescan
     * Clears cached claims and per-file verification records so the next scan
     * re-reads every JAR's in-JAR manifest. Does not itself re-run plugin loading.
     */
    @PostMapping("/plugins/verification-status/rescan")
    public ResponseEntity<Void> rescan() {
        LOG.info("REST request to rescan plugin verification state");
        pluginVerifier.rescan();
        return ResponseEntity.ok().build();
    }
}