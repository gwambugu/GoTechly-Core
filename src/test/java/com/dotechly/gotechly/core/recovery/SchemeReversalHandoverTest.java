package com.dotechly.gotechly.core.recovery;

import com.dotechly.gotechly.core.domain.DeferredRecovery;
import com.dotechly.gotechly.core.repository.DeferredRecoveryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static com.dotechly.gotechly.core.recovery.DeferredRecoveryService.SchemeReversal.HANDLED_BY_RECOVERY;
import static com.dotechly.gotechly.core.recovery.DeferredRecoveryService.SchemeReversal.PROCEED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The reconciler and R-NDPS's own pacs.007 both back out a cancelled credit; only one of them may.
 * A DFS wallet has no duplicate guard of its own, so getting this wrong debits the wallet twice.
 * Also pins the grace period that gives the pacs.007 its chance to arrive first.
 */
class SchemeReversalHandoverTest {

    private final DeferredRecoveryRepository repository = mock(DeferredRecoveryRepository.class);
    private final RecoveryProperties properties = new RecoveryProperties();
    private final DeferredRecoveryService service = new DeferredRecoveryService(repository, properties);

    @Test
    void noRowLetsTheReversalProceed() {
        when(repository.findByRequestId("E2E-1")).thenReturn(Optional.empty());
        assertEquals(PROCEED, service.handOverToScheme("E2E-1"));
    }

    /** The normal case: the pacs.007 arrived inside the grace period. */
    @Test
    void anUnstartedFailedRowIsSupersededAndTheReversalProceeds() {
        DeferredRecovery row = row(RecoveryModel.DeclaredOutcome.FAILED, RecoveryModel.RecoveryState.PENDING, 0);

        assertEquals(PROCEED, service.handOverToScheme("E2E-1"));
        assertEquals(RecoveryModel.RecoveryState.CANCELLED, row.getState());
        verify(repository).saveAndFlush(row);
    }

    @Test
    void losingTheCancelToAWorkerClaimLeavesItToTheReconciler() {
        row(RecoveryModel.DeclaredOutcome.FAILED, RecoveryModel.RecoveryState.PENDING, 0);
        when(repository.saveAndFlush(any())).thenThrow(new OptimisticLockingFailureException("claimed"));

        assertEquals(HANDLED_BY_RECOVERY, service.handOverToScheme("E2E-1"));
    }

    /** Any attempt may already have sent a reversal; a later RETRY would hide it in lastAction. */
    @Test
    void aStartedFailedRowIsHandledByRecovery() {
        DeferredRecovery row = row(RecoveryModel.DeclaredOutcome.FAILED, RecoveryModel.RecoveryState.PENDING, 1);
        row.setLastAction(RecoveryModel.Action.RETRY);

        assertEquals(HANDLED_BY_RECOVERY, service.handOverToScheme("E2E-1"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void aFirstAttemptInFlightIsHandledByRecovery() {
        DeferredRecovery row = row(RecoveryModel.DeclaredOutcome.FAILED, RecoveryModel.RecoveryState.IN_PROGRESS, 0);
        row.setClaimedAt(Instant.now());

        assertEquals(HANDLED_BY_RECOVERY, service.handOverToScheme("E2E-1"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void aReconciledFailedRowIsHandledByRecovery() {
        row(RecoveryModel.DeclaredOutcome.FAILED, RecoveryModel.RecoveryState.RECONCILED, 1);
        assertEquals(HANDLED_BY_RECOVERY, service.handOverToScheme("E2E-1"));
    }

    /** An UNKNOWN row (callback unconfirmed) was escalated without any attempt — the pacs.007 settles it. */
    @Test
    void anEscalatedUnknownRowIsClosedByTheSchemeReversal() {
        DeferredRecovery row = row(RecoveryModel.DeclaredOutcome.UNKNOWN, RecoveryModel.RecoveryState.NEEDS_ATTENTION, 0);

        assertEquals(PROCEED, service.handOverToScheme("E2E-1"));
        assertEquals(RecoveryModel.RecoveryState.CANCELLED, row.getState());
    }

    /** A CBS-timeout row would repost; the scheme has now cancelled, so that target is gone. */
    @Test
    void aSuccessRowIsCancelledEvenAfterAttempts() {
        DeferredRecovery row = row(RecoveryModel.DeclaredOutcome.SUCCESS, RecoveryModel.RecoveryState.PENDING, 2);

        assertEquals(PROCEED, service.handOverToScheme("E2E-1"));
        assertEquals(RecoveryModel.RecoveryState.CANCELLED, row.getState());
    }

    @Test
    void aCancelledRowIsLeftAlone() {
        row(RecoveryModel.DeclaredOutcome.FAILED, RecoveryModel.RecoveryState.CANCELLED, 3);

        assertEquals(PROCEED, service.handOverToScheme("E2E-1"));
        verify(repository, never()).saveAndFlush(any());
    }

    // ------------------------------------------------------------------ grace period

    @Test
    void aCancelledCallbackWaitsOutTheGracePeriod() {
        properties.setTriggerDelays(Map.of("rndps-callback-cancelled", Duration.ofSeconds(60)));
        when(repository.findByRequestId("E2E-1")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        DeferredRecovery row = service.enqueue("E2E-1", "RNDPS", "inward",
                RecoveryModel.DeclaredOutcome.FAILED, " RNDPS-Callback-Cancelled ");

        assertTrue(!row.getNextAttemptAt().isBefore(before.plusSeconds(60)),
                "first attempt must not come before the grace period, got " + row.getNextAttemptAt());
    }

    @Test
    void otherTriggersAreAttemptedAtOnce() {
        properties.setTriggerDelays(Map.of("rndps-callback-cancelled", Duration.ofSeconds(60)));
        when(repository.findByRequestId("E2E-1")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        DeferredRecovery row = service.enqueue("E2E-1", "RNDPS", "inward",
                RecoveryModel.DeclaredOutcome.SUCCESS, "cbc-payment-unconfirmed");

        assertTrue(row.getNextAttemptAt().isBefore(Instant.now().plusSeconds(1)));
        assertEquals(Duration.ZERO, properties.delayFor(null));
    }

    private DeferredRecovery row(RecoveryModel.DeclaredOutcome declared, RecoveryModel.RecoveryState state, int attempts) {
        DeferredRecovery row = new DeferredRecovery();
        row.setId(7L);
        row.setRequestId("E2E-1");
        row.setDeclaredOutcome(declared);
        row.setState(state);
        row.setAttempts(attempts);
        when(repository.findByRequestId("E2E-1")).thenReturn(Optional.of(row));
        return row;
    }
}
