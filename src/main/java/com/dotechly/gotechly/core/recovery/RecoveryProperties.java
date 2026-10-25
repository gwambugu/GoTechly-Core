package com.dotechly.gotechly.core.recovery;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@code gotechly.recovery.*} — knobs for the deferred reconciler.
 *
 * <p>Defaults are chosen for a corridor whose core banking occasionally goes away for tens of
 * minutes: patient rather than eager, because every attempt costs a real posting round-trip and a
 * core that is down does not get better for being asked more often.
 */
@ConfigurationProperties(prefix = "gotechly.recovery")
public class RecoveryProperties {

    /** Master switch. Off means the stage records nothing and no workers start. */
    private boolean enabled = true;

    /**
     * How many transactions the in-memory working set holds. Overflow is <b>not</b> rejected — the
     * durable row is already written, so overflow only means "the sweeper will pick it up", and the
     * high-water WARN is the signal that core banking is in trouble.
     */
    private int queueCapacity = 500;

    /** Worker threads. This is the real concurrency limit against core banking. */
    private int workers = 4;

    /** Give up after this many attempts. */
    private int maxAttempts = 12;

    /**
     * Hard stop regardless of attempts. Whichever of the two trips first sends the row to
     * {@code NEEDS_ATTENTION}.
     */
    private Duration giveUpAfter = Duration.ofHours(6);

    /** How long a claim is honoured before another instance may take the row. */
    private Duration leaseDuration = Duration.ofMinutes(5);

    /** Sweeper cadence: re-hydrates due rows and escalates expired ones. */
    private Duration sweepInterval = Duration.ofSeconds(60);

    /** Rows the sweeper pulls per pass. */
    private int sweepBatchSize = 200;

    /**
     * Declared outcome to assume when the transaction records none — no
     * {@code Details.additionalFields.declaredOutcome} stamp and a status that says nothing.
     *
     * <p><b>Unset by default, and unset means escalate rather than guess.</b> The declared outcome
     * decides which repair runs, and the two repairs move money in opposite directions: guess
     * {@code FAILED} against a credit the scheme was told had settled and the reconciler reverses it;
     * guess {@code SUCCESS} against one the scheme rejected and it posts money nobody will settle.
     * Neither is a defensible answer to "we do not know", so an undetermined transaction is recorded
     * as {@link RecoveryModel.DeclaredOutcome#UNKNOWN} and sent to
     * {@link RecoveryModel.RecoveryState#NEEDS_ATTENTION} for a human.
     *
     * <p>There is no globally safe value to put here, which is why there is no default. The right
     * answer depends on what the corridor's callback declares on a timeout, and that differs per
     * corridor — R-NDPS answers {@code ACSC}, so {@code FAILED} would be exactly backwards for it.
     * Setting this is therefore an explicit assertion by the operator about one deployment's
     * corridors, not a safety net.
     *
     * <p>The real fix for a corridor that keeps landing here is to stamp
     * {@code Details.additionalFields.declaredOutcome} in its callback handler, as
     * {@code RndpsInboundCallbackHandler} does — then this is never consulted at all.
     */
    private RecoveryModel.DeclaredOutcome defaultDeclaredOutcome;

    /**
     * Halt codes from a <b>repair</b> phase that mean the repair will never succeed, however many
     * times it is tried. The row escalates to {@link RecoveryModel.RecoveryState#NEEDS_ATTENTION}
     * immediately instead of spending its remaining attempts re-asking a question core banking has
     * already answered.
     *
     * <p>The motivating case is a business-rule decline: a customer under a credit restriction makes
     * {@code cbc-txn-payment} return DE39 {@code 119} on every repost until an operator lifts it.
     * Today that burns all twelve attempts over six hours and lands in the same terminal state, just
     * later and after a dozen pointless postings.
     *
     * <p><b>Only definitive halts belong here.</b> A timeout must never be listed: it means the
     * posting may have landed, and the next attempt's gate is what settles it. That distinction is
     * {@code CbsResponseCodes}' job — {@code cbc-payment-unconfirmed} is the timeout code and
     * {@code cbc-payment-failed} the decline, so listing the wrong one converts a self-healing retry
     * into a human's problem. Same for {@code cbc-reversal-unconfirmed}.
     *
     * <p><b>Defaults cover both repairs.</b> {@code cbc-reversal-failed} joined the list once
     * {@code TransactionReversalStage} stopped overloading it with <i>"Transaction already
     * reversed"</i> — that case now raises {@code cbc-reversal-already-done} and is handled by
     * {@link #alreadySatisfiedHaltCodes}. Until the split, listing it here would have escalated rows
     * that were in fact reconciled.
     *
     * <p>Configurable because a repair phase's stages come from plugin config: a corridor whose
     * {@code recovery-repost} runs {@code out-payment-handler>MPESA} rather than {@code cbc-txn-payment}
     * raises entirely different halt codes, and hard-coding the CBS ones would quietly make this
     * engine Finacle-specific.
     */
    private Set<String> terminalHaltCodes = new LinkedHashSet<>(List.of(
            "cbc-payment-failed",
            "cbs-payment-acc-missing",
            "cbc-reversal-failed",
            "cbc-reversal-payment-missing"));

    /**
     * Halt codes from a repair phase that mean <b>the end state the repair wanted already holds</b>.
     * The row is reconciled, not retried and not escalated.
     *
     * <p>A halt normally means the repair did not take. This is the exception: {@code cbc-txn-reversal}
     * halts {@code cbc-reversal-already-done} when core banking reports the entry is already backed
     * out — which is precisely what the reconciler was trying to achieve. It gets there when a
     * duplicate pacs.007 reversed the entry first, or when an earlier attempt landed and lost its
     * answer to a timeout.
     *
     * <p>Getting this wrong in either direction is expensive: treated as a failure it retries a
     * reversal that cannot happen twice and eventually calls a human to a reconciled row; treated as
     * terminal it does the same immediately. Both would put the row in {@code NEEDS_ATTENTION} with
     * nothing wrong with it, which is how an alert queue stops being believed.
     *
     * <p>Checked <b>before</b> {@link #terminalHaltCodes}, so a code in both lists is read as
     * satisfied. That ordering is deliberate — the safe reading of an ambiguous code is the one that
     * does not raise a false alarm.
     */
    private Set<String> alreadySatisfiedHaltCodes = new LinkedHashSet<>(List.of(
            "cbc-reversal-already-done"));

    private final Backoff backoff = new Backoff();
    private final Phases phases = new Phases();

    /** Exponential backoff with jitter. */
    public static class Backoff {
        private Duration initial = Duration.ofSeconds(30);
        private double multiplier = 2.0;
        private Duration max = Duration.ofMinutes(10);
        /**
         * Fraction of the computed delay to scatter randomly, 0..1. Without it a core-banking outage
         * synchronises every waiting transaction onto the same instant and the recovery pass arrives
         * as one thundering herd against a host that has only just come back.
         */
        private double jitter = 0.2;

        public Duration getInitial() { return initial; }
        public void setInitial(Duration initial) { this.initial = initial; }

        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double multiplier) { this.multiplier = multiplier; }

        public Duration getMax() { return max; }
        public void setMax(Duration max) { this.max = max; }

        public double getJitter() { return jitter; }
        public void setJitter(double jitter) { this.jitter = jitter; }
    }

    /**
     * Pipeline phases the engine resolves per payment type, exactly like any other apiType. A phase
     * left unconfigured for a payment type resolves to no stages, which disables that repair for that
     * corridor — an inward-only deployment can legitimately have no repost path.
     */
    public static class Phases {
        /** Runs the gate. Normally {@code [cbc-txn-status]}. */
        private String gate = "recovery-gate";
        /** Declared FAILED, core banking posted. Normally {@code [cbc-txn-reversal, ...]}. */
        private String reverse = "recovery-reverse";
        /** Declared SUCCESS, core banking has nothing. Normally {@code [cbc-txn-payment, ...]}. */
        private String repost = "recovery-repost";
        /** Runs once when the two already agree — bookkeeping and notifications. */
        private String confirm = "recovery-confirm";

        public String getGate() { return gate; }
        public void setGate(String gate) { this.gate = gate; }

        public String getReverse() { return reverse; }
        public void setReverse(String reverse) { this.reverse = reverse; }

        public String getRepost() { return repost; }
        public void setRepost(String repost) { this.repost = repost; }

        public String getConfirm() { return confirm; }
        public void setConfirm(String confirm) { this.confirm = confirm; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getQueueCapacity() { return queueCapacity; }
    public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }

    public int getWorkers() { return workers; }
    public void setWorkers(int workers) { this.workers = workers; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public Duration getGiveUpAfter() { return giveUpAfter; }
    public void setGiveUpAfter(Duration giveUpAfter) { this.giveUpAfter = giveUpAfter; }

    public Duration getLeaseDuration() { return leaseDuration; }
    public void setLeaseDuration(Duration leaseDuration) { this.leaseDuration = leaseDuration; }

    public Duration getSweepInterval() { return sweepInterval; }
    public void setSweepInterval(Duration sweepInterval) { this.sweepInterval = sweepInterval; }

    public int getSweepBatchSize() { return sweepBatchSize; }
    public void setSweepBatchSize(int sweepBatchSize) { this.sweepBatchSize = sweepBatchSize; }

    public RecoveryModel.DeclaredOutcome getDefaultDeclaredOutcome() { return defaultDeclaredOutcome; }
    public void setDefaultDeclaredOutcome(RecoveryModel.DeclaredOutcome defaultDeclaredOutcome) {
        this.defaultDeclaredOutcome = defaultDeclaredOutcome;
    }

    public Backoff getBackoff() { return backoff; }

    public Set<String> getTerminalHaltCodes() { return terminalHaltCodes; }

    public void setTerminalHaltCodes(Set<String> terminalHaltCodes) {
        this.terminalHaltCodes = terminalHaltCodes == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(terminalHaltCodes);
    }

    /**
     * Whether a repair phase's halt code means "stop, a human is needed".
     *
     * <p>Comparison rules are in {@link RecoveryProperties#matches}.
     */
    public boolean isTerminalHaltCode(String haltCode) {
        return matches(terminalHaltCodes, haltCode);
    }

    /**
     * Trimmed, case-insensitive membership, like the pipeline's other configured code lists.
     *
     * <p>A halt code is hand-written in two places — the stage that raises it and the list here — and
     * a stray space or a capital between them would silently turn the rule off, which is the exact
     * shape of bug this area keeps producing.
     *
     * <p><b>A blank halt code never matches anything.</b> A stage that halted without naming a code
     * has told us nothing, and "nothing" must not be read as either a refusal or a success.
     */
    private static boolean matches(Set<String> configured, String haltCode) {
        if (haltCode == null || haltCode.isBlank() || configured == null) {
            return false;
        }
        String candidate = haltCode.trim();
        return configured.stream()
                .filter(code -> code != null && !code.isBlank())
                .anyMatch(code -> code.trim().equalsIgnoreCase(candidate));
    }

    public Set<String> getAlreadySatisfiedHaltCodes() { return alreadySatisfiedHaltCodes; }

    public void setAlreadySatisfiedHaltCodes(Set<String> alreadySatisfiedHaltCodes) {
        this.alreadySatisfiedHaltCodes = alreadySatisfiedHaltCodes == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(alreadySatisfiedHaltCodes);
    }

    /** Whether a repair phase's halt code means the end state it wanted already holds. */
    public boolean isAlreadySatisfiedHaltCode(String haltCode) {
        return matches(alreadySatisfiedHaltCodes, haltCode);
    }

    public Phases getPhases() { return phases; }

    /**
     * Delay before attempt number {@code attempts + 1}, capped and jittered.
     *
     * @param attempts attempts already made (0 for the first retry)
     */
    public Duration backoffFor(int attempts) {
        double base = backoff.getInitial().toMillis()
                * Math.pow(backoff.getMultiplier(), Math.max(0, attempts));
        double capped = Math.min(base, backoff.getMax().toMillis());
        double spread = capped * backoff.getJitter();
        double jittered = capped + (Math.random() * 2 - 1) * spread;
        return Duration.ofMillis(Math.max(1L, (long) jittered));
    }
}
