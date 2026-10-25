package com.dotechly.gotechly.core.pipeline;

import com.dotechly.gotechly.core.pipeline.config.PipelineProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Discovers all {@link PipelineStage} {@code @Component} beans via DI,
 * and assembles ordered stage lists per (direction, payment type, API type) based on YAML configuration.
 * Caches assembled pipelines for fast lookup at request time.
 */
@Component
public class PipelineRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineRegistry.class);

    private final PipelineProperties properties;
    private final Map<String, PipelineStage> stagesByName;
    private final Map<String, List<PipelineStage>> pipelineCache = new ConcurrentHashMap<>();
    private final Map<String, List<PipelineStage>> compensationCache = new ConcurrentHashMap<>();

    /**
     * Spring injects all {@code @Component} beans that implement {@link PipelineStage}.
     */
    public PipelineRegistry(PipelineProperties properties, List<PipelineStage> allStages) {
        this.properties = properties;

        this.stagesByName = new LinkedHashMap<>();
        for (PipelineStage stage : allStages) {
            PipelineStage existing = stagesByName.put(stage.name(), stage);
            if (existing != null) {
                throw new IllegalStateException(
                        "Duplicate pipeline stage name '" + stage.name() + "': " +
                                existing.getClass().getName() + " vs " + stage.getClass().getName());
            }
        }

        LOG.info("Discovered {} pipeline stages: {}", stagesByName.size(), stagesByName.keySet());
    }

    @PostConstruct
    public void init() {
        // Pre-build pipelines for explicitly configured payment types
        for (Map.Entry<String, Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>>> ptEntry :
                properties.getPaymentTypes().entrySet()) {
            String paymentType = ptEntry.getKey();
            for (Map.Entry<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> dirEntry :
                    ptEntry.getValue().entrySet()) {
                String direction = dirEntry.getKey();
                for (Map.Entry<String, PipelineProperties.ApiTypeStagesConfig> apiEntry :
                        dirEntry.getValue().entrySet()) {
                    String apiType = apiEntry.getKey();
                    PipelineProperties.ApiTypeStagesConfig config = apiEntry.getValue();

                    // Pre-build main pipeline
                    List<String> stageNames = config.getStages();
                    String cacheKey = cacheKey(direction, paymentType, apiType);
                    pipelineCache.put(cacheKey, resolveStages(stageNames, direction, paymentType, apiType));

                    // Pre-build compensation pipelines
                    if (config.getCompensation() != null) {
                        for (Map.Entry<String, List<String>> compEntry : config.getCompensation().entrySet()) {
                            String haltCode = compEntry.getKey();
                            String compKey = compensationCacheKey(direction, paymentType, apiType, haltCode);
                            compensationCache.put(compKey, resolveStages(
                                    compEntry.getValue(), direction, paymentType,
                                    apiType + ":compensation:" + haltCode));
                        }
                    }
                }
            }
        }

        LOG.info("Pre-built pipelines: {}", pipelineCache.keySet());
        if (!compensationCache.isEmpty()) {
            LOG.info("Pre-built compensation pipelines: {}", compensationCache.keySet());
        }
        LOG.info("Default pipeline stages: {}", properties.getDefaultStages());
    }

    /**
     * Get the ordered list of pipeline stages for a direction, payment type, and API type.
     * Falls back to default-stages for that direction + API type if no specific config exists.
     */
    public List<PipelineStage> getPipeline(String direction, String paymentType, String apiType) {
        String key = cacheKey(direction, paymentType, apiType);
        return pipelineCache.computeIfAbsent(key, k -> {
            LOG.info("Building default pipeline for direction='{}', paymentType='{}', apiType='{}'",
                    direction, paymentType, apiType);
            Map<String, List<String>> directionDefaults = properties.getDefaultStages().get(direction);
            if (directionDefaults == null) {
                throw new IllegalStateException(
                        "No default stages configured for direction '" + direction +
                                "'. Check gotechly.pipeline.default-stages." + direction);
            }
            List<String> defaults = directionDefaults.get(apiType);
            if (defaults == null || defaults.isEmpty()) {
                throw new IllegalStateException(
                        "No default stages configured for direction '" + direction +
                                "', apiType '" + apiType +
                                "'. Check gotechly.pipeline.default-stages." + direction + "." + apiType);
            }
            return resolveStages(defaults, direction, paymentType, apiType);
        });
    }

    /**
     * Get the compensation pipeline for a specific halt code within a phase.
     * Returns null if no compensation pipeline is configured for this combination.
     */
    public List<PipelineStage> getCompensationPipeline(String direction, String paymentType,
                                                       String apiType, String haltCode) {
        String key = compensationCacheKey(direction, paymentType, apiType, haltCode);
        return compensationCache.get(key);
    }

    /** Maximum allowed compensation nesting depth from configuration. */
    public int getMaxCompensationDepth() {
        return properties.getMaxCompensationDepth();
    }

    /**
     * Register pipeline config for a payment type at runtime (called by PluginLoader).
     * Skips any direction+apiType combination that already exists from application.yml
     * (application.yml has higher priority than plugin config).
     */
    public void registerPaymentTypePipeline(String paymentType,
                                            Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> directionConfig) {

        Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> appYml =
                properties.getPaymentTypes().get(paymentType);

        for (Map.Entry<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> dirEntry :
                directionConfig.entrySet()) {
            String direction = dirEntry.getKey();
            for (Map.Entry<String, PipelineProperties.ApiTypeStagesConfig> apiEntry :
                    dirEntry.getValue().entrySet()) {
                String apiType = apiEntry.getKey();

                if (appYml != null && appYml.containsKey(direction)
                        && appYml.get(direction).containsKey(apiType)) {
                    LOG.info("Skipping plugin pipeline for {}:{}/{} — overridden by application.yml",
                            paymentType, direction, apiType);
                    continue;
                }

                PipelineProperties.ApiTypeStagesConfig config = apiEntry.getValue();
                String key = cacheKey(direction, paymentType, apiType);
                pipelineCache.put(key, resolveStages(config.getStages(), direction, paymentType, apiType));

                if (config.getCompensation() != null) {
                    for (Map.Entry<String, List<String>> compEntry : config.getCompensation().entrySet()) {
                        String haltCode = compEntry.getKey();
                        String compKey = compensationCacheKey(direction, paymentType, apiType, haltCode);
                        compensationCache.put(compKey, resolveStages(
                                compEntry.getValue(), direction, paymentType,
                                apiType + ":compensation:" + haltCode));
                    }
                }

                List<String> stages = config.getStages();
                LOG.info("Registered plugin pipeline: {}:{}/{} — {} stage(s): {}",
                        paymentType, direction, apiType, stages.size(), stages);
            }
        }
    }

    /**
     * Deregister all plugin-contributed pipeline config for a payment type.
     * Entries that originated from application.yml are preserved.
     */
    public void deregisterPaymentTypePipeline(String paymentType) {
        Map<String, Map<String, PipelineProperties.ApiTypeStagesConfig>> appYml =
                properties.getPaymentTypes().get(paymentType);

        String ptToken = ":" + paymentType + ":";
        pipelineCache.keySet().removeIf(key -> {
            if (!key.contains(ptToken)) return false;
            if (appYml != null) {
                String[] parts = key.split(":");
                if (parts.length >= 3 && appYml.containsKey(parts[0])
                        && appYml.get(parts[0]).containsKey(parts[2])) {
                    return false;
                }
            }
            return true;
        });
        compensationCache.keySet().removeIf(key -> {
            if (!key.contains(ptToken)) return false;
            if (appYml != null) {
                String[] parts = key.split(":");
                if (parts.length >= 3 && appYml.containsKey(parts[0])
                        && appYml.get(parts[0]).containsKey(parts[2])) {
                    return false;
                }
            }
            return true;
        });
        LOG.info("Deregistered plugin pipeline config for paymentType '{}'", paymentType);
    }

    /** Clear cached pipelines (useful after hot-reload or config change). */
    public void clearCache() {
        pipelineCache.clear();
        compensationCache.clear();
    }

    /** Returns the set of registered stage names for admin/diagnostics. */
    public Set<String> getRegisteredStageNames() {
        return Collections.unmodifiableSet(stagesByName.keySet());
    }

    private String cacheKey(String direction, String paymentType, String apiType) {
        return direction + ":" + paymentType + ":" + apiType;
    }

    private String compensationCacheKey(String direction, String paymentType, String apiType, String haltCode) {
        return direction + ":" + paymentType + ":" + apiType + ":compensation:" + haltCode;
    }

    private List<PipelineStage> resolveStages(List<String> stageNames, String direction, String paymentType, String apiType) {
        List<PipelineStage> resolved = new ArrayList<>();
        for (String nameSpec : stageNames) {
            // Support delegate syntax: "out-async-validation-handler>MPESA"
            // The part after '>' is the delegate payment type for handler registry lookup
            String stageName;
            String delegatePaymentType = null;
            int delegateIdx = nameSpec.indexOf('>');
            if (delegateIdx > 0 && delegateIdx < nameSpec.length() - 1) {
                stageName = nameSpec.substring(0, delegateIdx);
                delegatePaymentType = nameSpec.substring(delegateIdx + 1);
            } else {
                stageName = nameSpec;
            }

            PipelineStage stage = stagesByName.get(stageName);
            if (stage == null) {
                throw new IllegalStateException(
                        "Pipeline stage '" + stageName + "' referenced in config for direction '" + direction +
                                "', paymentType '" + paymentType +
                                "', apiType '" + apiType +
                                "' not found. Available stages: " + stagesByName.keySet());
            }

            if (delegatePaymentType != null) {
                stage = new DelegatingStageWrapper(stage, delegatePaymentType);
                LOG.info("Stage '{}' will delegate to paymentType '{}' for handler lookup",
                        stageName, delegatePaymentType);
            }

            resolved.add(stage);
        }
        return Collections.unmodifiableList(resolved);
    }
}