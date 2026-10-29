package com.dotechly.gotechly.core.recovery;

import com.dotechly.gotechly.core.domain.DeferredRecovery;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineExecutor;
import com.dotechly.gotechly.core.pipeline.PipelineRegistry;
import com.dotechly.gotechly.core.pipeline.PipelineStage;
import com.dotechly.gotechly.spi.service.PaymentLogLookup;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A DFS wallet credit must be reconciled by the wallet phases, never by the CBS ones: core banking
 * cannot see the wallet, so its gate would answer "not posted" and close a row with money still in it.
 */
class RecoveryProfilePhaseTest {

    private static final String DIR = "inward";
    private static final String PT = "RNDPS";

    private final PipelineRegistry registry = mock(PipelineRegistry.class);
    private final PipelineExecutor executor = mock(PipelineExecutor.class);
    private final PaymentLogLookup lookup = mock(PaymentLogLookup.class);
    private final DeferredRecoveryService service = mock(DeferredRecoveryService.class);
    private final DeferredRecoveryEngine engine = new DeferredRecoveryEngine(
            () -> registry, () -> executor, lookup, service, new RecoveryProperties());
    private final PipelineStage stage = mock(PipelineStage.class);

    @Test
    void aWalletCreditIsGatedAndReversedByTheDfsPhases() {
        log("dfs-wallet");
        configured("recovery-gate-dfs-wallet", "recovery-reverse-dfs-wallet");

        engine.attempt(row(RecoveryModel.DeclaredOutcome.FAILED));

        verify(registry).getPipeline(DIR, PT, "recovery-gate-dfs-wallet");
        verify(registry).getPipeline(DIR, PT, "recovery-reverse-dfs-wallet");
        verify(registry, never()).getPipeline(DIR, PT, "recovery-gate");
        verify(service).reconciled(eq(1L), eq(RecoveryModel.CbsState.POSTED), eq(RecoveryModel.Action.REVERSE),
                eq("recovery-reverse-dfs-wallet"), anyString());
    }

    @Test
    void anUnconfiguredProfilePhaseFallsBackToTheBase() {
        log("dfs-wallet");
        configured("recovery-gate-dfs-wallet");
        when(registry.getPipeline(DIR, PT, "recovery-reverse")).thenReturn(List.of(stage));

        engine.attempt(row(RecoveryModel.DeclaredOutcome.FAILED));

        verify(service).reconciled(eq(1L), eq(RecoveryModel.CbsState.POSTED), eq(RecoveryModel.Action.REVERSE),
                eq("recovery-reverse"), anyString());
    }

    /** {@code stages: []} says "never" — falling back to recovery-repost would post to core banking. */
    @Test
    void aProfilePhaseConfiguredEmptyDoesNotFallBack() {
        log("dfs-wallet");
        configured("recovery-gate-dfs-wallet");
        when(registry.isConfigured(DIR, PT, "recovery-repost-dfs-wallet")).thenReturn(true);
        when(registry.getPipeline(DIR, PT, "recovery-repost-dfs-wallet")).thenReturn(List.of());
        doAnswer(inv -> {
            PipelineContext ctx = inv.getArgument(1);
            ctx.halt("dfs-credit-status", "DFS-S2", "cbc-status-txn-not-found", "not held");
            return inv.getArgument(2);
        }).when(executor).execute(any(), any(), any());

        engine.attempt(row(RecoveryModel.DeclaredOutcome.SUCCESS));

        verify(registry, never()).getPipeline(DIR, PT, "recovery-repost");
        verify(service).escalate(eq(1L), anyString());
    }

    @Test
    void noProfileUsesTheBasePhases() {
        log(null);
        when(registry.getPipeline(DIR, PT, "recovery-gate")).thenReturn(List.of(stage));
        when(registry.getPipeline(DIR, PT, "recovery-reverse")).thenReturn(List.of(stage));

        engine.attempt(row(RecoveryModel.DeclaredOutcome.FAILED));

        verify(registry, never()).isConfigured(any(), any(), any());
        verify(service).reconciled(eq(1L), eq(RecoveryModel.CbsState.POSTED), eq(RecoveryModel.Action.REVERSE),
                eq("recovery-reverse"), anyString());
    }

    private void configured(String... phases) {
        for (String phase : phases) {
            when(registry.isConfigured(DIR, PT, phase)).thenReturn(true);
            when(registry.getPipeline(DIR, PT, phase)).thenReturn(List.of(stage));
        }
    }

    private void log(String profile) {
        PaymentLogSPI log = new PaymentLogSPI();
        Details details = new Details();
        details.setRequestId("E2E-1");
        details.setAdditionalFields(new HashMap<>());
        if (profile != null) {
            details.getAdditionalFields().put("recoveryProfile", profile);
        }
        log.setDetails(details);
        when(lookup.findByOriginalRequestId("E2E-1")).thenReturn(Optional.of(log));
        when(executor.execute(any(), any(), any())).thenAnswer(inv -> inv.getArgument(2));
    }

    private static DeferredRecovery row(RecoveryModel.DeclaredOutcome declared) {
        DeferredRecovery row = new DeferredRecovery();
        row.setId(1L);
        row.setRequestId("E2E-1");
        row.setPaymentType(PT);
        row.setDirection(DIR);
        row.setDeclaredOutcome(declared);
        row.setAttempts(0);
        return row;
    }
}
