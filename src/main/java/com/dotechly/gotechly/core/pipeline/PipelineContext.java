package com.dotechly.gotechly.core.pipeline;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Mutable context passed through all pipeline stages for a single request.
 * Carries metadata, timing info, and inter-stage communication.
 * One instance is created per request — not shared across requests.
 */
public class PipelineContext {

    private final String paymentType;
    private final String direction;
    private final String pipelinePhase;
    private final Instant startedAt;
    private final Map<String, Object> attributes = new HashMap<>();
    private final int compensationDepth;
    private String rawResult;
    private String rawResultResponse;
    private boolean halted = false;
    private String haltReason;
    private String haltedByStage;
    private String haltReasonCode;
    private String haltCode;

    public PipelineContext(String paymentType, String direction, String pipelinePhase) {
        this(paymentType, direction, pipelinePhase, 0);
    }

    PipelineContext(String paymentType, String direction, String pipelinePhase, int compensationDepth) {
        this.paymentType = paymentType;
        this.direction = direction;
        this.pipelinePhase = pipelinePhase;
        this.compensationDepth = compensationDepth;
        this.startedAt = Instant.now();
    }

    public String getPaymentType() { return paymentType; }

    /**
     * Returns the effective payment type for handler registry lookups.
     * If a delegate is set (by a stage with {@code >DelegateType} syntax), returns the delegate.
     * Otherwise returns the request's own payment type.
     */
    public String getEffectivePaymentType() {
        String delegate = getAttribute("delegatePaymentType");
        return delegate != null ? delegate : paymentType;
    }

    public String getDirection() { return direction; }

    public String getPipelinePhase() { return pipelinePhase; }

    public Instant getStartedAt() { return startedAt; }

    /** Stages can store arbitrary data for downstream stages. */
    public void setAttribute(String key, Object value) { attributes.put(key, value); }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }

    /**
     * Mark pipeline as halted with separate error code and halt code.
     *
     * @param stageName      the stage that halted the pipeline
     * @param haltReasonCode technical error code for the response (e.g., "PF-06")
     * @param haltCode       semantic halt code for compensation routing (e.g., "payment-failed")
     * @param reason         human-readable description
     */
    public void halt(String stageName, String haltReasonCode, String haltCode, String reason) {
        this.halted = true;
        this.haltedByStage = stageName;
        this.haltReason = reason;
        this.haltReasonCode = haltReasonCode;
        this.haltCode = haltCode;
    }

    /**
     * Backward-compatible halt — uses haltReasonCode as the haltCode.
     */
    public void halt(String stageName, String haltReasonCode, String reason) {
        halt(stageName, haltReasonCode, haltReasonCode, reason);
    }

    public boolean isHalted() { return halted; }

    public String getHaltReason() { return haltReason; }

    public String getHaltReasonCode() { return haltReasonCode; }

    /** Semantic halt code used for compensation pipeline routing. */
    public String getHaltCode() { return haltCode; }

    public String getHaltedByStage() { return haltedByStage; }

    /** Current compensation nesting depth. 0 = main pipeline, 1+ = compensation pipeline. */
    public int getCompensationDepth() { return compensationDepth; }

    /**
     * Create a new context for a compensation pipeline, inheriting payment metadata
     * and incrementing the compensation depth.
     */
    public PipelineContext deriveCompensation(String compensationPhase) {
        PipelineContext ctx = new PipelineContext(
                this.paymentType, this.direction, compensationPhase, this.compensationDepth + 1);
        // Copy attributes so compensation stages can see original pipeline state
        ctx.attributes.putAll(this.attributes);
        return ctx;
    }

    /** Raw result string from the payment provider callback (used by result-processing stages). */
    public String getRawResult() { return rawResult; }

    public void setRawResult(String rawResult) { this.rawResult = rawResult; }

    /** Response string to return to the external provider after result processing. */
    public String getRawResultResponse() { return rawResultResponse; }

    public void setRawResultResponse(String rawResultResponse) { this.rawResultResponse = rawResultResponse; }

}
