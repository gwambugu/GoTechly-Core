package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.participants.reversal;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsNotPlacedException;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsQueueResolver;
import com.dotechly.gotechly.spi.service.dto.payment.CBS;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsExchange;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.jpos.util.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TransactionReversalStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionReversalStage.class);

    @Override
    public String name() {
        return "cbc-txn-reversal";
    }

    @Override
    public int defaultOrder() {
        return 50;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        LOG.debug("[CBS Finacle cbc] Transaction reversal for paymentType '{}', requestId '{}'",
                context.getPaymentType(),
                request.getDetails() != null ? request.getDetails().getRequestId() : "unknown");
        if (request.getCbs() == null || request.getCbs().getRequest() == null) {
            String reasonCode = "CBC-119";
            context.halt(name(), reasonCode, "cbc-reversal-payment-missing", "Missing payment request");
            // halt() only sets flags, it does not stop the stage. Without this return we would post
            // to CBS with no reversal request and the else branch below would overwrite the halt
            // code, so cbc-reversal-payment-missing could never reach its compensation.
            return request;
        }
        if ("REVERSED".equals(request.getCbs().getStatus())) {
            // Its own halt code, not cbc-reversal-failed. Both stop the pipeline, but they mean
            // opposite things: "failed" is core banking refusing to back the entry out, while this is
            // core banking reporting the entry is ALREADY out — the caller's desired end state, just
            // reached by someone else (a duplicate pacs.007, or an earlier recovery attempt that
            // landed and lost its answer). Sharing one code forced every consumer to treat a
            // completed reversal as a failure; the deferred reconciler in particular would escalate a
            // row that is in fact reconciled, calling a human to look at nothing.
            context.halt(name(), "CBC-913", "cbc-reversal-already-done", "Transaction already reversed");
            return request;
        }
        PaymentLogSPI response;
        try {
            response = placePaymentRequestToSpace(request, "TXN_REVERSAL",
                    CbsQueueResolver.countryCode(request));
        } catch (CbsNotPlacedException e) {
            // Never left this process, so the original posting is untouched and still needs
            // reversing. Unconfirmed, not failed: the caller must retry rather than give up.
            LOG.error("[CBS Finacle cbc] reversal not placed for requestId '{}': {}",
                    request.getDetails() != null ? request.getDetails().getRequestId() : "unknown",
                    e.getMessage());
            context.halt(name(), "CBC-119", "cbc-reversal-unconfirmed", e.getMessage());
            return request;
        }
        if (response == null || response.getCbs() == null) {
            // Nothing usable came back: the jPOS space timed out (context.get returns null), or
            // appQBean was not registered. We cannot know whether the reversal landed, so halt as
            // unconfirmed on the request we were handed rather than dereferencing the response.
            context.halt(name(), "CBC-911", "cbc-reversal-unconfirmed",
                    response == null ? "No response from CBS" : "CBS response carried no CBS block");
            return request;
        }
        request = response;
        if ("REVERSED".equals(request.getCbs().getStatus())) {
            return request;
        } else {
            CBS cbs = request.getCbs() == null ? new CBS() : request.getCbs();
            String reversalResponseCode = (cbs.getAdditionalFields() == null) ? null : request.getCbs().getAdditionalFields().get("cbc.payment.reversal.response.DE39");
            String reversalRef = (cbs.getAdditionalFields() == null) ? null : request.getCbs().getAdditionalFields().get("cbc.payment.reversal.response.DE127");
            String reasonCode = reversalResponseCode == null ? "CBC-911" : "CBC-" + reversalResponseCode;
            String haltCode = reversalRef == null ? "cbc-reversal-unconfirmed" : "cbc-reversal-failed";
            String reason = request.getCbs().getRespDesc() == null ? "Time out occured" : request.getCbs().getRespDesc();
            context.halt(name(), reasonCode, haltCode, reason);
        }

        return request;
    }

    public PaymentLogSPI placePaymentRequestToSpace(PaymentLogSPI paymentLogSPI,
                                                    String logEventDescription,
                                                    String countryCode) {
        LOG.info("Placing transaction reversal request to space: {}", paymentLogSPI);
        AppQBean appQBean = CbsQueueResolver.appQBean();

        String spaceN = appQBean.getConfiguration().get("input-space");
        Long timeout = appQBean.getConfiguration().getLong("spaceTimeout");
        String realm = "INQ";
        LogEvent event = new LogEvent(realm, logEventDescription);
        Date now = new Date();

        // Thread-safe selection of queue
        String queueName = CbsQueueResolver.nextQueue(countryCode);

        LOG.info("Placing requestId='{}' to queueName={}", paymentLogSPI.getDetails().getRequestId(), queueName);
        event.addMessage("Transaction ID: " + paymentLogSPI.getDetails().getRequestId());
        event.addMessage("Start Time: " + now);

        Context context = new Context();
        Space<String, Context> space = SpaceFactory.getSpace(spaceN);

        CbsExchange.put(context, "CBC_REVERSAL_REQUEST", paymentLogSPI);
        context.put("CBC_MSG_TYPE", "CBC_TRANSACTION_REVERSAL");
        context.put("timestamp", now);

        // Place request into space
        space.out(queueName, context, timeout);

        // Wait for response (thread-safe)
        return CbsExchange.await(context, "CBC_REVERSAL_RESPONSE", 20000);
    }
}
