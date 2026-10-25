package com.dotechly.gotechly.core.pipeline.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

@ConfigurationProperties(prefix = "gotechly.pipeline")
public class PipelineProperties {

    /**
     * Default stages per direction and API type, applied when a payment type
     * does not have a specific override for that direction + API type.
     * Key = direction ("inward" / "outward"),
     * value = map of API type ("validation", "payment-processing", etc.) to ordered stage names.
     */
    private Map<String, Map<String, List<String>>> defaultStages = Map.of(
            "inward", Map.of(
                    "validation", List.of("in-validation-handler"),
                    "payment-processing", List.of("in-payment-handler"),
                    "result-processing", List.of("in-result-handler"),
                    "status-processing", List.of("in-status-handler")
            ),
            "outward", Map.of(
                    "validation", List.of("out-validation-handler"),
                    "payment-processing", List.of("out-payment-handler"),
                    "result-processing", List.of("out-result-handler"),
                    "status-processing", List.of("out-status-handler")
            )
    );

    /**
     * Per-payment-type pipeline configuration.
     * Key = payment type (e.g., "MPESA"),
     * value = direction map → API type map → stage config.
     */
    private Map<String, Map<String, Map<String, ApiTypeStagesConfig>>> paymentTypes = Map.of();

    /**
     * Maximum compensation pipeline nesting depth. Prevents infinite recursion
     * when compensation stages themselves halt and trigger further compensation.
     * Default: 3.
     */
    private int maxCompensationDepth = 3;

    /** Answering a status enquiry from the stored transaction when its outcome is already final. */
    private StatusShortCircuit statusShortCircuit = new StatusShortCircuit();

    public Map<String, Map<String, List<String>>> getDefaultStages() { return defaultStages; }

    public void setDefaultStages(Map<String, Map<String, List<String>>> defaultStages) { this.defaultStages = defaultStages; }

    public Map<String, Map<String, Map<String, ApiTypeStagesConfig>>> getPaymentTypes() { return paymentTypes; }

    public void setPaymentTypes(Map<String, Map<String, Map<String, ApiTypeStagesConfig>>> paymentTypes) { this.paymentTypes = paymentTypes; }

    public int getMaxCompensationDepth() { return maxCompensationDepth; }

    public void setMaxCompensationDepth(int maxCompensationDepth) { this.maxCompensationDepth = maxCompensationDepth; }

    public StatusShortCircuit getStatusShortCircuit() { return statusShortCircuit; }

    public void setStatusShortCircuit(StatusShortCircuit statusShortCircuit) {
        this.statusShortCircuit = (statusShortCircuit == null) ? new StatusShortCircuit() : statusShortCircuit;
    }

    /**
     * Controls whether {@code db-logger} answers a status enquiry straight from the stored
     * transaction instead of letting the pipeline continue to the status handler.
     *
     * <p>An outward payment is acknowledged as PENDING and settled later by the provider's result
     * callback, which stamps the final outcome on the row. Once that has happened the provider has
     * nothing left to tell us, so a status enquiry is a pure read of our own DB — asking the
     * provider again costs a round-trip and risks a stale or differently-worded answer overwriting
     * a settled transaction.
     *
     * <p>{@code final-statuses} is the vocabulary of terminal {@link
     * com.dotechly.gotechly.spi.service.dto.payment.Details#getStatus() Details.status}
     * values, matched case-insensitively and trimmed. Anything not listed — including {@code
     * PENDING}, a blank status and any code ops has not classified yet — is treated as still in
     * flight and goes on to the handler, which is the safe direction to be wrong in.
     */
    public static class StatusShortCircuit {

        private boolean enabled = true;

        private Set<String> finalStatuses = normalize(List.of(
                "COMPLETED", "SUCCESS", "FAILED", "REJECTED",
                // ISO 20022 TxSts as stored by the RNDPS/Pesalink corridors. ACSP
                // ("accepted, settlement in process") is deliberately absent — it is in flight.
                "ACSC", "ACCC", "RJCT"));

        public boolean isEnabled() { return enabled; }

        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public Set<String> getFinalStatuses() { return finalStatuses; }

        public void setFinalStatuses(Set<String> finalStatuses) {
            this.finalStatuses = normalize(finalStatuses);
        }

        /** True when {@code status} is a terminal outcome and the enquiry can be answered from the DB. */
        public boolean isFinalStatus(String status) {
            if (!enabled || status == null) {
                return false;
            }
            return finalStatuses.contains(status.trim().toUpperCase(Locale.ROOT));
        }

        private static Set<String> normalize(Iterable<String> values) {
            Set<String> out = new LinkedHashSet<>();
            if (values != null) {
                for (String value : values) {
                    if (value != null && !value.isBlank()) {
                        out.add(value.trim().toUpperCase(Locale.ROOT));
                    }
                }
            }
            return out;
        }
    }

    public static class ApiTypeStagesConfig {
        private List<String> stages;

        /**
         * Compensation pipelines keyed by halt code (e.g., "payment-failed").
         * Each entry maps a halt code to an ordered list of stage names to execute
         * when the main pipeline halts with that code.
         */
        private Map<String, List<String>> compensation;

        public List<String> getStages() { return stages; }

        public void setStages(List<String> stages) { this.stages = stages; }

        public Map<String, List<String>> getCompensation() { return compensation; }

        public void setCompensation(Map<String, List<String>> compensation) { this.compensation = compensation; }
    }
}
