package com.dotechly.gotechly.core.pipeline.stages.aml;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Anti-Money Laundering screening stage.
 * Runs BEFORE the plugin handler to block suspicious transactions
 * before they reach the payment provider.
 *
 * TODO: Integrate with AML screening service (watchlist checks,
 * transaction pattern analysis, sanctions screening).
 *
 * When a transaction is flagged:
 * <pre>
 *   request.getDetails().setResponseCode("AML01");
 *   request.getDetails().setResponseDesc("Transaction blocked by AML screening");
 *   request.getDetails().setStatus("BLOCKED");
 *   throw new PipelineHaltException("aml-screening", "AML01", "Sender on watchlist", request);
 * </pre>
 */
@Component
public class AmlScreeningStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(AmlScreeningStage.class);

    @Override
    public String name() {
        return "aml-screening";
    }

    @Override
    public int defaultOrder() {
        return 100;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        LOG.debug("[AML] Screening transaction for paymentType '{}', requestId '{}' -- NO-OP placeholder",
                context.getPaymentType(),
                request.getDetails() != null ? request.getDetails().getRequestId() : "unknown");
        if(request.getDetails().getRequestId().equals("00000000")){
            String reasonCode ="AML-01";
            String reason = "AML error";
            //halt(String stageName, String haltReasonCode, String haltCode, String reason)
            context.halt("db-logger",reasonCode ,"aml-blocked",reason);
            request.getDetails().setStatus("FAILED");
            request.getDetails().setResponseCode(reasonCode);
            request.getDetails().setResponseDesc(reason);
            return request;
        }

        // Placeholder — pass through unchanged
        return request;
    }
}
