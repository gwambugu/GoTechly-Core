package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.participants.payment;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsNotPlacedException;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsQueueResolver;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsResponseCodes;
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

/**
 * {@code cbc-txn-payment} — posts the money movement to Finacle over jPOS.
 *
 * <p>The invariant this stage holds is <b>it never leaves without a halt code</b>. The halt code
 * selects the compensation pipeline that answers the scheme, and {@code PipelineExecutor} does not
 * mark the context halted when a stage throws: it logs, breaks the loop, and
 * {@code executeCompensationIfNeeded} returns early because {@code isHalted()} is false. An escaping
 * exception therefore means the scheme is told nothing at all.
 *
 * <p>Three outcomes, and the difference between the last two is the whole consistency problem:
 * <ul>
 *   <li><b>posted</b> — no halt.</li>
 *   <li><b>{@code cbc-payment-failed}</b> — <em>definitively</em> not posted. Either Finacle declined
 *       with a response code that is neither success nor timeout, or the request never left this
 *       process ({@link CbsNotPlacedException}). Safe to declare FAIL to the scheme with nothing to
 *       reverse.</li>
 *   <li><b>{@code cbc-payment-unconfirmed}</b> — the posting may have landed. Whatever is declared to
 *       the scheme, core banking has to be reconciled against it afterwards via
 *       {@code cbc-txn-status}.</li>
 * </ul>
 *
 * <p><b>An absent response code is unconfirmed, never failed.</b> A MUX timeout leaves
 * {@code CBS.status = TIMEOUT} and {@code respCode} null, and the previous {@code !"000".equals(...)}
 * test read that empty string as "some code other than success" and halted
 * {@code cbc-payment-failed} — declaring a transfer that may well be on the books definitively
 * rejected, so the scheme was told FAIL and no reversal was ever raised. That is the exact
 * inconsistency this corridor exists to avoid.
 *
 * <p><b>So is every one of the host's timeout codes.</b> Which codes those are is
 * {@link CbsResponseCodes}'s answer, not this stage's — {@code TransactionPaymentParticipant} derives
 * {@code CBS.status} from the same field and the two have to agree, because the {@code POSTED} check
 * above runs before the code is ever looked at. Until 2026-08-31 only {@code 911} was recognised, so
 * {@code 906}, {@code 907} and {@code 909} halted {@code cbc-payment-failed}: R-NDPS was told RJCT
 * and, since that halt code carries no {@code deferred-recovery} stage, no recovery row was raised
 * either. A posting that had in fact landed would then stay wrong for good, with nothing scheduled to
 * find it.
 *
 * <p><b>The status wins over the response code on a timeout.</b> The participant writes
 * {@code CBS.status} on every path out of {@code commit()}; it writes {@code CBS.respCode} only when
 * a response actually arrived. So a {@code TIMEOUT} status with a response code present means the
 * code belongs to an earlier stage on the same {@code PaymentLogSPI}, and the deferred reconciler
 * produces exactly that: {@code DeferredRecoveryEngine.repair} hands this stage the log
 * {@code cbc-txn-status} has just enriched in place. A repost that timed out therefore arrived
 * carrying the inquiry's own answer â {@code respCode "000(F)"}, {@code respDesc "Transaction Not
 * found"} â which {@link CbsResponseCodes} reads as an unrecognised DE39, hence {@code DECLINED},
 * hence {@code cbc-payment-failed}. That halt code is in {@code gotechly.recovery.terminal-halt-codes},
 * so the reconciler escalated the row to {@code NEEDS_ATTENTION} on its first attempt rather than
 * rescheduling it, and the repost never got its remaining tries. Fixed on both sides: the
 * participant clears the response fields it is not going to write, and the check below means no
 * earlier stage's leftovers can outvote this attempt's own verdict.
 */
@Component
public class TransactionPaymentStage implements PipelineStage {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionPaymentStage.class);

    @Override
    public String name() {
        return "cbc-txn-payment";
    }

    @Override
    public int defaultOrder() {
        return 50;
    }

    @Override
    public PaymentLogSPI execute(PipelineContext context, PaymentLogSPI request) {
        LOG.debug("[CBS Finacle cbc] Transaction payment for paymentType '{}', requestId '{}'",
                context.getPaymentType(),
                request.getDetails() != null ? request.getDetails().getRequestId() : "unknown");
        if (request.getCbs() == null) {
            request.setCbs(new CBS());
        }
        if (request.getDetails() != null && "IFT".equals(request.getDetails().getPaymentType())) {
            if (request.getCbs().getDrAcc() == null && request.getSender() != null) {
                request.getCbs().setDrAcc(request.getSender().getAccountNumber());
            }
            if (request.getCbs().getCrAcc() == null && request.getReceiver() != null) {
                request.getCbs().setCrAcc(request.getReceiver().getAccountNumber());
            }
        }
        if (request.getCbs().getDrAcc() == null || request.getCbs().getCrAcc() == null) {
            String reasonCode = "CBC-114";
            context.halt(name(), reasonCode, "cbs-payment-acc-missing", "Missing Credit or Debit Account");
            // halt() only sets flags, it does not stop the stage. Without this return we would
            // post to CBS with a null Dr/Cr account and the branches below would overwrite the halt
            // code, so cbs-payment-acc-missing could never reach its compensation.
            return request;
        }

        PaymentLogSPI response;
        try {
            response = placePaymentRequestToSpace(request, "TXN_PAYMENT",
                    CbsQueueResolver.countryCode(request));
        } catch (CbsNotPlacedException e) {
            // Resolved before anything was placed: nothing reached core banking, so a reject is
            // truthful and no reversal will be needed.
            LOG.error("[CBS Finacle cbc] payment not placed for requestId '{}': {}",
                    request.getDetails() != null ? request.getDetails().getRequestId() : "unknown",
                    e.getMessage());
            context.halt(name(), "CBC-119", "cbc-payment-failed", e.getMessage());
            return request;
        }
        if (response == null || response.getCbs() == null) {
            // Nothing usable came back: the jPOS space timed out (context.get returns null). We
            // cannot know whether the posting landed, so halt as unconfirmed on the request we were
            // handed rather than dereferencing the response.
            context.halt(name(), "CBC-911", "cbc-payment-unconfirmed",
                    response == null ? "No response from CBS" : "CBS response carried no CBS block");
            return request;
        }
        request = response;

        CBS cbs = request.getCbs();
        String cbsResponseCode = cbs.getRespCode() == null ? "" : cbs.getRespCode().trim();
        String cbsStatus = cbs.getStatus() == null ? "" : cbs.getStatus();

        if ("POSTED".equals(cbsStatus)) {
            return request;
        }
        // The participant writes CBS.status on every path out of commit(), including the one where
        // core banking never answered; CBS.respCode it writes only when a response actually came
        // back. So when the status says timeout, that is this attempt's verdict and the response
        // code may belong to some earlier stage on the same PaymentLogSPI â which is exactly what
        // the deferred reconciler produces, since it hands cbc-txn-payment the log cbc-txn-status
        // just enriched. Classifying the stale code read "000(F)" (the status inquiry's own
        // decorated form) as an unrecognised DE39, hence DECLINED, hence cbc-payment-failed: a
        // terminal halt code that escalated a repost timeout to NEEDS_ATTENTION on attempt 1
        // instead of rescheduling it. Trusting the status here means no earlier stage's leftovers
        // can ever outvote it.
        if ("TIMEOUT".equals(cbsStatus) || "CHARGE-TIMEOUT".equals(cbsStatus)) {
            // Quote the code back only when it really is one of the host's timeout codes. Anything
            // else present on a timed-out attempt was written by an earlier stage rather than this
            // one, and reporting it would put a status inquiry's "000(F)" on the payment's audit
            // trail as though core banking had answered the posting with it.
            String reasonCode = CbsResponseCodes.isTimeoutCode(cbsResponseCode) ? cbsResponseCode : "911";
            context.halt(name(), "CBC-" + reasonCode, "cbc-payment-unconfirmed",
                    "CBS did not answer the posting (status '" + cbsStatus + "')");
            return request;
        }
        switch (CbsResponseCodes.classify(cbsResponseCode)) {
            case TIMEOUT -> {
                // No code at all, or one of the host's own timeout codes. Either way the posting is
                // in an unknown state: the request reached Finacle and may have been applied, so it
                // must be reconciled, not written off. This halt code is what puts deferred-recovery
                // on the compensation — cbc-payment-failed has no such stage.
                context.halt(name(), "CBC-" + (cbsResponseCode.isEmpty() ? "911" : cbsResponseCode),
                        "cbc-payment-unconfirmed",
                        cbsResponseCode.isEmpty()
                                ? "No response code from CBS (status '" + cbsStatus + "')"
                                : "CBS reported timeout " + cbsResponseCode);
            }
            case DECLINED -> context.halt(name(), "CBC-" + cbsResponseCode, "cbc-payment-failed",
                    cbs.getRespDesc() == null
                            ? "CBS declined the posting with " + cbsResponseCode : cbs.getRespDesc());
            // Success code but the participant did not mark it POSTED — the two disagree, so nothing
            // is settled.
            case SUCCESS -> context.halt(name(), "CBC-" + cbsResponseCode, "cbc-payment-unconfirmed",
                    "CBS returned " + cbsResponseCode + " but status is '" + cbsStatus + "'");
        }
        return request;
    }

    public PaymentLogSPI placePaymentRequestToSpace(PaymentLogSPI paymentLogSPI,
                                                    String logEventDescription,
                                                    String countryCode) {
        LOG.info("Placing transaction payment to space: {}", paymentLogSPI);
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

        CbsExchange.put(context, "CBC_PAYMENT_REQUEST", paymentLogSPI);
        context.put("CBC_MSG_TYPE", "CBC_TRANSACTION_PAYMENT");
        context.put("timestamp", now);

        // Place request into space
        space.out(queueName, context, timeout);

        // Wait for response (thread-safe)
        return CbsExchange.await(context, "CBC_PAYMENT_RESPONSE", 20000);
    }
}
