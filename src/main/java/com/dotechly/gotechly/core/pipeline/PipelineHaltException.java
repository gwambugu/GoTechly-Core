package com.dotechly.gotechly.core.pipeline;

import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;

/**
 * Thrown by a {@link PipelineStage} to immediately halt pipeline execution.
 * The executor catches this, marks the context as halted, and returns
 * the partial result without calling further stages.
 *
 * <p>Carries two distinct codes:
 * <ul>
 *   <li>{@code errorCode} — technical code returned to the caller (e.g., "PF-06")</li>
 *   <li>{@code haltCode} — semantic code that drives compensation pipeline routing (e.g., "payment-failed")</li>
 * </ul>
 */
public class PipelineHaltException extends RuntimeException {

    private final String stageName;
    private final String errorCode;
    private final String haltCode;
    private final PaymentLogSPI partialResult;

    /**
     * Full constructor with separate error code and halt code.
     *
     * @param stageName     the stage that threw this exception
     * @param errorCode     technical error code for the response (e.g., "PF-06")
     * @param haltCode      semantic halt code for compensation routing (e.g., "payment-failed")
     * @param message       human-readable description
     * @param partialResult the PaymentLogSPI state at time of halt, or null
     */
    public PipelineHaltException(String stageName, String errorCode, String haltCode,
                                 String message, PaymentLogSPI partialResult) {
        super(message);
        this.stageName = stageName;
        this.errorCode = errorCode;
        this.haltCode = haltCode;
        this.partialResult = partialResult;
    }

    /**
     * Backward-compatible constructor — uses errorCode as the haltCode.
     */
    public PipelineHaltException(String stageName, String errorCode, String message, PaymentLogSPI partialResult) {
        this(stageName, errorCode, errorCode, message, partialResult);
    }

    public String getStageName() { return stageName; }

    public String getErrorCode() { return errorCode; }

    /** Semantic halt code used for compensation pipeline routing. */
    public String getHaltCode() { return haltCode; }

    public PaymentLogSPI getPartialResult() { return partialResult; }
}
