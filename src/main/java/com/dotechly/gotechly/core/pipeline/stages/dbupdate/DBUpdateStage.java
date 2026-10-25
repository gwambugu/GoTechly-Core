package com.dotechly.gotechly.core.pipeline.stages.dbupdate;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.service.impl.PaymentLogService;
import com.dotechly.gotechly.core.util.PaymentSpiConverter;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Writes a provider result back onto the originating transaction.
 *
 * <p>Result handlers come in two shapes. Some (MPESA, Ecash) hydrate the original themselves via
 * {@code PaymentLogLookup.findByOriginalRequestId} and return a fully-populated log that already
 * carries its database id — for those this stage simply saves. Others (the RNDPS pacs.002 codec)
 * parse the provider payload into the seed log the protocol resource created, which has no id; that
 * object is a <em>result</em>, not a record, and saving it directly inserts a duplicate row instead
 * of updating the payment.
 *
 * <p>So the stage reconciles: when the log has no id it is matched to the stored transaction by
 * {@code requestId} and merged onto it by {@link PaymentLogResultMerger}. An unmatched result is
 * never inserted — an orphan row carrying an outcome for a payment we have no record of is worse
 * than a loud failure — so the pipeline halts with {@code DB-U1} and ops can reconcile from the
 * audit trail.
 */
@Component
public class DBUpdateStage implements PipelineStage {
    private static final Logger LOG = LoggerFactory.getLogger(DBUpdateStage.class);

    private static final String UNMATCHED_RESULT = "DB-U1";
    private static final String NO_CORRELATION_ID = "DB-U2";

    private final PaymentLogService paymentLogService;
    private final PaymentLogResultMerger resultMerger;

    public DBUpdateStage(PaymentLogService paymentLogService, PaymentLogResultMerger resultMerger) {
        this.paymentLogService = paymentLogService;
        this.resultMerger = resultMerger;
    }

    @Override
    public String name() {
        return "db-update";
    }

    @Override
    public int defaultOrder() {
        return 1001;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String paymentType = context.getPaymentType();

        if (request == null || request.getDetails() == null) {
            LOG.error("[DB-UPDATE] No Details on the result for paymentType '{}' — nothing to reconcile",
                    paymentType);
            return request;
        }

        String requestId = request.getDetails().getRequestId();
        LOG.debug("[DB-UPDATE] paymentType '{}', requestId '{}', id {}",
                paymentType, requestId, request.getId());

        // Already hydrated by the handler: it holds the stored record with the outcome applied.
        if (request.getId() != null) {
            return persist(request);
        }

        if (requestId == null || requestId.isBlank()) {
            LOG.error("[DB-UPDATE] Result for paymentType '{}' carries no requestId — cannot match it to a "
                    + "transaction. Check that the result handler maps the provider's correlation id "
                    + "(pacs.002 OrgnlEndToEndId) onto Details.requestId.", paymentType);
            return halt(context, request, NO_CORRELATION_ID, "Result carries no correlation id");
        }

        Optional<PaymentLogSPI> stored = paymentLogService.findByOriginalRequestId(requestId);
        if (stored.isEmpty()) {
            LOG.error("[DB-UPDATE] No transaction found for requestId '{}' (paymentType '{}') — refusing to "
                    + "insert an orphan result row", requestId, paymentType);
            return halt(context, request, UNMATCHED_RESULT, "Original transaction Not found");
        }

        PaymentLogSPI reconciled = resultMerger.merge(stored.orElseThrow(), request);
        LOG.info("[DB-UPDATE] Reconciled result onto transaction requestId '{}' (id {}), status '{}'",
                requestId, reconciled.getId(), reconciled.getDetails().getStatus());
        return persist(reconciled);
    }

    private PaymentLogSPI persist(PaymentLogSPI log) {
        Optional<PaymentLogSPI> saved = paymentLogService.update(log);
        // update() always returns a value today; fall back to the merged object rather than the
        // null this stage used to emit, which propagated an NPE into every downstream stage.
        return saved.orElse(log);
    }

    private PaymentLogSPI halt(PipelineContext context, PaymentLogSPI request, String reasonCode, String reason) {
        context.halt(name(), reasonCode, reason);
        request.getDetails().setStatus("FAILED");
        request.getDetails().setResponseCode(reasonCode);
        request.getDetails().setResponseDesc(reason);
        return request;
    }
}
