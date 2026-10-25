package com.dotechly.gotechly.core.pipeline.stages.dblogger;

import com.dotechly.gotechly.core.config.InstanceInfo;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.pipeline.config.PipelineProperties;
import com.dotechly.gotechly.core.service.dto.PaymentLogDTO;
import com.dotechly.gotechly.core.service.impl.PaymentLogService;
import com.dotechly.gotechly.core.util.PaymentSpiConverter;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DBLogger implements PipelineStage {
    private static final Logger log = LoggerFactory.getLogger(DBLogger.class);

    private final PaymentLogService paymentLogService;
    private final PaymentSpiConverter paymentSpiConverter;
    private final InstanceInfo instanceInfo;
    private final PipelineProperties.StatusShortCircuit statusShortCircuit;

    public DBLogger(PaymentLogService paymentLogService, PaymentSpiConverter paymentSpiConverter,
                    InstanceInfo instanceInfo, PipelineProperties pipelineProperties) {
        this.paymentLogService = paymentLogService;
        this.paymentSpiConverter = paymentSpiConverter;
        this.instanceInfo = instanceInfo;
        this.statusShortCircuit = pipelineProperties.getStatusShortCircuit();
    }

    @Override
    public String name() {
        return "db-logger";
    }

    @Override
    public int defaultOrder() {
        return 50;
    }

    /** Phases that open a transaction, and so must reject a replayed requestId. */
    private static final String VALIDATION_PHASE = "validation";
    private static final String PAYMENT_PHASE = "payment-processing";
    private static final String ASYNC_VALIDATION_PHASE = "async-validation";
    private static final String STATUS_PHASE = "status-processing";

    /**
     * Halt code used when a status enquiry is answered from an already-settled transaction. It is a
     * successful short-circuit, not a failure, so nothing should register a compensation pipeline
     * against it — doing so would compensate a transaction that completed normally.
     */
    public static final String STATUS_FINAL_HALT_CODE = "status-already-final";

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        if (request.getDetails() == null) {
            log.error("[DB-LOGGER] No Details on the request for paymentType '{}' — nothing to persist",
                    context.getPaymentType());
            return request;
        }

        String phase = context.getPipelinePhase();
        String requestId = request.getDetails().getRequestId();
        log.debug("[DB-LOGGER] phase '{}', paymentType '{}', requestId '{}'",
                phase, context.getPaymentType(), requestId);

        request.getDetails().setAppInstanceId(instanceInfo.getInstanceId());
        request.getDetails().setAppHost(instanceInfo.getHostname());

        // Keyed on the pipeline phase, not Details.requestType: the caller owns requestType and its
        // vocabulary is corridor-specific (OUT_STATUS, STATUS_REQUEST, …), whereas the phase is
        // decided by the endpoint that dispatched the pipeline.
        if (STATUS_PHASE.equals(phase)) {
            return loadOriginalTransaction(context, request, requestId);
        }
        return openTransaction(context, request, phase);
    }

    /**
     * A status enquiry runs against a transaction that already exists: load it and bump the status
     * counter, so the rest of the pipeline works from the stored transaction rather than whatever
     * the caller echoed in the status request.
     *
     * <p>If the stored transaction has already settled — the provider's result callback stamped a
     * final status on it — the enquiry is answered from the DB and the pipeline halts here, so the
     * status handler never makes the provider round-trip. Only a transaction still in flight is
     * carried on to the handler. See {@link PipelineProperties.StatusShortCircuit}.
     */
    private PaymentLogSPI loadOriginalTransaction(PipelineContext context, PaymentLogSPI request, String requestId) {
        Optional<PaymentLogSPI> original = paymentLogService.fetchOriginalRequest(requestId);
        if (original.isEmpty()) {
            log.warn("[DB-LOGGER] Status enquiry for unknown requestId '{}'", requestId);
            return halt(context, request, "DB-O1", "Original transaction Not found");
        }

        PaymentLogSPI stored = original.orElseThrow();
        String storedStatus = (stored.getDetails() == null) ? null : stored.getDetails().getStatus();
        if (statusShortCircuit.isFinalStatus(storedStatus)) {
            log.info("[DB-LOGGER] Status enquiry for requestId '{}' answered from the stored transaction — "
                    + "status '{}' is final, skipping the provider enquiry", requestId, storedStatus);
            // Not a failure: the halt only stops the remaining stages. The stored status,
            // response code and description are left exactly as the result callback wrote them,
            // because they are the answer being returned.
            context.halt(name(), "DB-S1", STATUS_FINAL_HALT_CODE,
                    "Transaction already settled with status '" + storedStatus + "'");
        }
        return stored;
    }

    /**
     * Creates the transaction row. On the entry phases a duplicate requestId is a replay and halts
     * the pipeline; on a later phase the row was simply created upstream (or by a direct-to-payment
     * call that skipped validation), so the pipeline carries on with the request in hand.
     */
    private PaymentLogSPI openTransaction(PipelineContext context, PaymentLogSPI request, String phase) {
        Optional<PaymentLogSPI> paymentLogSPIOptional = paymentLogService.findByRequestIdInAllTables(request.getDetails().getRequestId());
        if(paymentLogSPIOptional.isEmpty()){
            Optional<PaymentLogSPI> saved = paymentLogService.saveNewRequest2(request);
            if (saved.isPresent()) {
                return saved.orElseThrow();
            }
        }else{
            if (VALIDATION_PHASE.equals(phase)
                    || ASYNC_VALIDATION_PHASE.equals(phase)
                    || PAYMENT_PHASE.equals(phase)
                    || "accept-deferred-flow".equals(phase)
            ) {
                PaymentLogSPI originalRequest  = paymentLogSPIOptional.get();
                log.info("[DB-LOGGER] Row already exists for requestId '{}' in phase '{}' — halting", originalRequest.getDetails().getRequestId(), phase);
                return halt(context, originalRequest, "DB-O2", "Duplicate error");
            }
            log.debug("[DB-LOGGER] Row already exists for requestId '{}' in phase '{}' — continuing",
                    request.getDetails().getRequestId(), phase);

        }

        return request;

    }

    private PaymentLogSPI halt(PipelineContext context, PaymentLogSPI request, String reasonCode, String reason) {
        context.halt("db-logger", reasonCode, reason);
        request.getDetails().setStatus("FAILED");
        request.getDetails().setResponseCode(reasonCode);
        request.getDetails().setResponseDesc(reason);
        return request;
    }
}