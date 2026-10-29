package com.dotechly.gotechly.core.pipeline.stages.callback;

import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The three stages that let the reconciler back out a DFS wallet credit: the gate that reads the
 * wallet state off the transaction, the stage that turns it into a reversal callback, and the
 * reading of DFS's answer.
 */
class DfsWalletRecoveryStagesTest {

    private final DfsCreditStatusStage gate = new DfsCreditStatusStage();
    private final DfsReversalPrepareStage prepare = new DfsReversalPrepareStage();
    private final DfsCallbackResponseStage response = new DfsCallbackResponseStage();

    // ------------------------------------------------------------------ gate

    @Test
    void anAcceptedWalletCreditIsHeld() {
        PipelineContext ctx = context();
        gate.execute(ctx, credit("SUCCESS", "0"));
        assertFalse(ctx.isHalted(), "no halt = POSTED to the engine: a reversal is owed");
    }

    @Test
    void aReversedWalletCreditIsNotHeld() {
        PaymentLogSPI log = credit("SUCCESS", "0");
        log.getDetails().addAdditionalField(DfsCallbackResponseStage.REVERSAL_STATUS_FIELD, "SUCCESS");
        PipelineContext ctx = context();

        gate.execute(ctx, log);

        assertEquals("cbc-status-txn-not-found", ctx.getHaltCode());
    }

    @Test
    void aRefusedWalletCreditIsNotHeld() {
        PipelineContext ctx = context();
        gate.execute(ctx, credit(null, "1003"));
        assertEquals("cbc-status-txn-not-found", ctx.getHaltCode());
    }

    @Test
    void noRecordedVerdictIsUnknown() {
        PipelineContext ctx = context();
        gate.execute(ctx, credit(null, null));
        assertEquals(DfsCreditStatusStage.UNKNOWN_HALT_CODE, ctx.getHaltCode());
    }

    // ------------------------------------------------------------------ prepare

    /** A retry must reuse the id, so DFS refuses a reversal that had in fact landed instead of applying it. */
    @Test
    void theConversationIdIsDeterministicAcrossAttempts() {
        PaymentLogSPI first = credit("SUCCESS", "0");
        prepare.execute(context(), first);
        PaymentLogSPI retry = credit("SUCCESS", "0");
        prepare.execute(context(), retry);

        assertEquals(DfsCallbackResponseStage.DFS_REVERSAL_REQUEST_TYPE, first.getDetails().getRequestType());
        assertEquals("RVE2E-1", first.getDetails().getAdditionalField("callback.pac007reversalid"));
        assertEquals(first.getDetails().getAdditionalField("callback.pac007reversalid"),
                retry.getDetails().getAdditionalField("callback.pac007reversalid"));
    }

    // ------------------------------------------------------------------ reversal reply

    @Test
    void anAcceptedReversalIsRecordedWithoutTouchingTheCreditVerdict() {
        PaymentLogSPI log = reversal();
        PipelineContext ctx = answered("<Result><ResultCode>0</ResultCode><ResultDesc>OK</ResultDesc></Result>");

        response.execute(ctx, log);

        assertFalse(ctx.isHalted());
        assertEquals("SUCCESS", log.getDetails().getAdditionalField(DfsCallbackResponseStage.REVERSAL_STATUS_FIELD));
        assertEquals("SUCCESS", log.getDetails().getAdditionalField(DfsCallbackResponseStage.CREDIT_STATUS_FIELD),
                "the credit's own verdict is what the gate reads — a reversal reply must not overwrite it");
        assertNull(ctx.getAttribute(CallbackStage.RESPONSE_ATTR), "consumed exactly once");
    }

    @Test
    void aRefusedReversalHaltsTerminally() {
        PipelineContext ctx = answered("<Result><ResultCode>AG45021</ResultCode><ResultDesc>Duplicate</ResultDesc></Result>");
        response.execute(ctx, reversal());
        assertEquals(DfsCallbackResponseStage.REVERSAL_REJECTED_HALT_CODE, ctx.getHaltCode());
    }

    @Test
    void noReplyToAReversalIsUnconfirmed() {
        PipelineContext ctx = context();
        response.execute(ctx, reversal());
        assertEquals(DfsCallbackResponseStage.REVERSAL_UNCONFIRMED_HALT_CODE, ctx.getHaltCode());
    }

    @Test
    void anUnreadableReplyToAReversalIsUnconfirmed() {
        PipelineContext ctx = answered("{\"not\":\"xml\"}");
        response.execute(ctx, reversal());
        assertEquals(DfsCallbackResponseStage.REVERSAL_UNCONFIRMED_HALT_CODE, ctx.getHaltCode());
    }

    @Test
    void theCreditBranchIsUnchangedByAMissingReply() {
        PipelineContext ctx = context();
        response.execute(ctx, credit(null, null));
        assertFalse(ctx.isHalted(), "no answer to a credit is still not a refusal");
    }

    // ------------------------------------------------------------------ fixtures

    private static PipelineContext context() {
        return new PipelineContext("RNDPS", "inward", "recovery-reverse-dfs-wallet");
    }

    private static PipelineContext answered(String body) {
        PipelineContext ctx = context();
        ctx.setAttribute(CallbackStage.RESPONSE_ATTR, new CallbackOutcome(200, body, "DFS", "text/xml"));
        return ctx;
    }

    private static PaymentLogSPI credit(String creditStatus, String creditCode) {
        PaymentLogSPI log = new PaymentLogSPI();
        Details details = new Details();
        details.setRequestId("E2E-1");
        details.setRequestType(DfsCallbackResponseStage.DFS_INWARD_REQUEST_TYPE);
        details.setAdditionalFields(new HashMap<>());
        if (creditStatus != null) details.addAdditionalField(DfsCallbackResponseStage.CREDIT_STATUS_FIELD, creditStatus);
        if (creditCode != null) details.addAdditionalField(DfsCallbackResponseStage.CREDIT_RESULT_CODE_FIELD, creditCode);
        log.setDetails(details);
        return log;
    }

    private static PaymentLogSPI reversal() {
        PaymentLogSPI log = credit("SUCCESS", "0");
        log.getDetails().setRequestType(DfsCallbackResponseStage.DFS_REVERSAL_REQUEST_TYPE);
        return log;
    }
}
