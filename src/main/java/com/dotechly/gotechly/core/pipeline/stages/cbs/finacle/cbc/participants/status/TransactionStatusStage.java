package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.participants.status;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsNotPlacedException;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsPaymentReference;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsQueueResolver;
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

/**
 * {@code cbc-txn-status} — asks Finacle whether a posting actually landed.
 *
 * <p>The inquiry protocol carries the answer in two fields, and they are not interchangeable:
 * <b>field 39 says whether core banking could serve the question at all</b> ({@code 000} = served),
 * and only then does <b>field 127 carry the state of the transaction</b>. Any other field 39 means
 * core banking had a problem and could not serve the request — it says nothing whatsoever about the
 * payment. So there are exactly <b>three</b> outcomes, never four:
 * <ul>
 *   <li><b>posted</b> — f39 {@code 000}, f127 flag {@code Y}. No halt, the pipeline continues.</li>
 *   <li><b>{@code cbc-status-txn-not-found}</b> — f39 {@code 000}, f127 flag {@code N}. Core banking
 *       answered, and answered "no such transaction". <em>Definitive</em>: the money did not
 *       move.</li>
 *   <li><b>{@code cbc-status-txn-unconfirmed}</b> — we still do not know. The inquiry timed out, f39
 *       came back non-{@code 000}, f127 carried no usable marker, or the transaction holds no
 *       reference to inquire on. The caller must retry rather than conclude anything.</li>
 * </ul>
 *
 * <p>There is deliberately no "the payment failed" branch: the protocol has no way to say that. It
 * used to exist, fed by a non-{@code 000} field 39 — reading a failure of the <em>question</em> as a
 * failure of the <em>payment</em>, which settled a possibly-posted transaction as rejected.
 *
 * <p>Only the definitive branch stamps {@code Details.status = FAILED}. Doing that on the unknown
 * branch — as this stage used to, in the same {@code else} that raised
 * {@code cbc-status-txn-unconfirmed} — declares a transaction failed on the strength of an inquiry
 * that returned nothing, which is exactly the inconsistency the inquiry exists to prevent.
 */
@Component
public class TransactionStatusStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionStatusStage.class);

    static final String UNCONFIRMED_HALT_CODE = "cbc-status-txn-unconfirmed";
    static final String NOT_FOUND_HALT_CODE = "cbc-status-txn-not-found";

    @Override
    public String name() {
        return "cbc-txn-status";
    }

    @Override
    public int defaultOrder() {
        return 50;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        String requestId = request.getDetails() != null ? request.getDetails().getRequestId() : "unknown";
        LOG.debug("[CBS Finacle cbc] transaction status for paymentType '{}', requestId '{}'",
                context.getPaymentType(), requestId);

        // tranRef (payment response field 127) when Finacle answered, else field 37 of the ISO
        // request that timed out. With neither there is nothing core banking can match on, and
        // asking anyway comes back "not found" — a definitive answer we have not earned.
        if (!CbsPaymentReference.isPresent(request)) {
            context.halt(name(), "CBC-100", UNCONFIRMED_HALT_CODE,
                    "No core-banking reference (tranRef or original RRN) to inquire on");
            // halt() only sets flags. Without this return the inquiry was still placed, keyed on
            // Details.requestId, which core banking has never seen.
            return request;
        }

        PaymentLogSPI response;
        try {
            response = placeStatusInquireToSpace(request, "TXN_INQ",
                    CbsQueueResolver.countryCode(request));
        } catch (CbsNotPlacedException e) {
            LOG.error("[CBS Finacle cbc] status inquiry not placed for requestId '{}': {}",
                    requestId, e.getMessage());
            context.halt(name(), "CBC-119", UNCONFIRMED_HALT_CODE, e.getMessage());
            return request;
        }

        if (response == null || response.getCbs() == null) {
            // Space timed out, or the participant could not build a reply. Unknown, not failed.
            context.halt(name(), "CBC-911", UNCONFIRMED_HALT_CODE,
                    response == null ? "No response from CBS" : "CBS response carried no CBS block");
            return request;
        }
        request = response;

        String cbsStatus = request.getCbs().getStatus();
        String respCode = request.getCbs().getRespCode();
        String respDesc = request.getCbs().getRespDesc();

        if ("POSTED".equals(cbsStatus)) {
            return request;
        }
        if ("NOT_FOUND".equals(cbsStatus)) {
            context.halt(name(), respCode == null ? "CBC-404" : "CBC-" + respCode,
                    NOT_FOUND_HALT_CODE,
                    respDesc == null ? "Transaction NOT FOUND" : respDesc);
            setStatusFailed(request);
            return request;
        }
        // NOT_CONFIRMED, or anything this stage does not recognise. Erring toward "in flight" is
        // deliberate — a state nobody has classified must not fail a transaction that may have posted.
        context.halt(name(), respCode == null ? "CBC-911" : "CBC-" + respCode,
                UNCONFIRMED_HALT_CODE,
                respDesc == null ? "Core banking did not confirm the transaction status" : respDesc);
        return request;
    }

    private static void setStatusFailed(PaymentLogSPI request) {
        if (request.getDetails() != null) {
            request.getDetails().setStatus("FAILED");
        }
    }

    public PaymentLogSPI placeStatusInquireToSpace(PaymentLogSPI paymentLogSPI,
                                                   String logEventDescription,
                                                   String countryCode) {
        LOG.info("Placing transaction inquire to space: {}", paymentLogSPI);
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

        CbsExchange.put(context, "CBC_STATUS_REQUEST", paymentLogSPI);
        context.put("CBC_MSG_TYPE", "CBC_TRANSACTION_STATUS");
        context.put("timestamp", now);

        // Place request into space
        space.out(queueName, context, timeout);

        // Wait for response (thread-safe)
        return CbsExchange.await(context, "CBC_STATUS_RESPONSE", 10000);
    }
}
