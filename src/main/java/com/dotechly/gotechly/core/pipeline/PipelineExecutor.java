package com.dotechly.gotechly.core.pipeline;

import com.dotechly.gotechly.core.pipeline.audit.PaymentLogSnapshotter;
import com.dotechly.gotechly.core.pipeline.audit.PipelineExecutionListener;
import com.dotechly.gotechly.core.pipeline.audit.StageExecutionRecord;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.util.common.PluginHaltException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
/**
 * Executes an ordered list of {@link PipelineStage} instances sequentially.
 * Handles short-circuiting via {@link PipelineContext#halt} or {@link PipelineHaltException}.
 * A plugin handler, which can see neither of those Core types, halts by throwing SPI's
 * {@link PluginHaltException}; the executor translates it here, stamping the name of the stage the
 * handler was running as (the delegating wrapper's display name, when one is in play).
 *
 * <p>Collects a {@link StageExecutionRecord} trail with before/after JSON snapshots
 * for each stage, and invokes all registered {@link PipelineExecutionListener} instances
 * after the pipeline completes — regardless of success, halt, or error.</p>
 *
 * <p>When a stage halts with a halt code that has a compensation pipeline configured,
 * the executor automatically triggers the compensation pipeline synchronously before
 * returning the result. Compensation pipelines can themselves trigger further compensation
 * up to the configured maximum depth.</p>
 */
@Component
public class PipelineExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(PipelineExecutor.class);

    /** Separator this executor stamps into a compensation context's phase name. */
    private static final String COMPENSATION_PHASE_MARKER = ":compensation:";

    private final PaymentLogSnapshotter snapshotter;
    private final List<PipelineExecutionListener> listeners;
    private final PipelineRegistry registry;

    public PipelineExecutor(PaymentLogSnapshotter snapshotter, List<PipelineExecutionListener> listeners,
                            PipelineRegistry registry) {
        this.snapshotter = snapshotter;
        this.listeners = listeners;
        this.registry = registry;
    }

    /**
     * Synchronous pipeline execution.
     */
    public PaymentLogSPI execute(List<PipelineStage> stages, PipelineContext context, PaymentLogSPI request) {
        PaymentLogSPI current = request;
        List<StageExecutionRecord> trail = new ArrayList<>();

        try {
            for (int i = 0; i < stages.size(); i++) {
                PipelineStage stage = stages.get(i);
                if (context.isHalted()) {
                    LOG.debug("Pipeline halted by '{}': {}. Skipping stage '{}'.",
                            context.getHaltedByStage(), context.getHaltReason(), stage.name());
                    recordSkipped(trail, stages.subList(i, stages.size()), context, current);
                    break;
                }

                LOG.debug("Executing pipeline stage '{}' for paymentType '{}'",
                        stage.name(), context.getPaymentType());

                String beforeSnapshot = snapshotter.snapshot(current);
                Instant stageStart = Instant.now();
                Throwable stageError = null;

                try {
                    current = stage.execute(context, current);
                } catch (PluginHaltException | PipelineHaltException e) {
                    PipelineHaltException halt = translateHalt(stage, e);
                    stageError = halt;
                    LOG.warn("Pipeline halted by stage '{}': errorCode={}, haltCode={}, message={}",
                            halt.getStageName(), halt.getErrorCode(), halt.getHaltCode(), halt.getMessage());
                    context.halt(halt.getStageName(), halt.getErrorCode(), halt.getHaltCode(), halt.getMessage());
                    current = halt.getPartialResult() != null ? halt.getPartialResult() : current;
                } catch (Exception ex) {
                    stageError = ex;
                    LOG.error("Unhandled error in pipeline stage '{}' for paymentType '{}': {}",
                            stage.name(), context.getPaymentType(), ex.getMessage(), ex);
                    current.getDetails().setResponseCode("GN-01");
                    current.getDetails().setResponseDesc("Payment gateway processing error");
                    current.getDetails().setStatus("PENDING");
                }

                Duration elapsed = Duration.between(stageStart, Instant.now());
                String afterSnapshot = snapshotter.snapshot(current);

                trail.add(new StageExecutionRecord(
                        stage.name(), stageStart, elapsed,
                        beforeSnapshot, afterSnapshot,
                        stageError, context.isHalted(),
                        context.getHaltReason(), context.getHaltReasonCode(),
                        context.getHaltCode()));

                if (stageError != null) {
                    recordSkipped(trail, stages.subList(i + 1, stages.size()), context, current);
                    break;
                }
            }

            // Compensation pipeline: trigger if halted with a matching compensation config
            current = executeCompensationIfNeeded(context, current, trail);

        } finally {
            notifyListeners(context, current, trail);
        }

        return current;
    }

    /**
     * Reactive pipeline execution.
     * Chains stages sequentially using flatMap, checking halt status between each.
     * Collects the audit trail and notifies listeners after the chain completes.
     */
    public Mono<PaymentLogSPI> executeReactive(List<PipelineStage> stages, PipelineContext context, PaymentLogSPI request) {
        List<StageExecutionRecord> trail = new ArrayList<>();
        Mono<PaymentLogSPI> chain = Mono.just(request);

        for (PipelineStage stage : stages) {
            chain = chain.flatMap(current -> {
                if (context.isHalted()) {
                    LOG.debug("Pipeline halted by '{}'. Skipping stage '{}'.",
                            context.getHaltedByStage(), stage.name());
                    trail.add(StageExecutionRecord.skipped(stage.name(), Instant.now(),
                            snapshotter.snapshot(current), context.getHaltReason(),
                            context.getHaltReasonCode(), context.getHaltCode()));
                    return Mono.just(current);
                }

                LOG.debug("Executing reactive pipeline stage '{}' for paymentType '{}'",
                        stage.name(), context.getPaymentType());

                String beforeSnapshot = snapshotter.snapshot(current);
                Instant stageStart = Instant.now();

                return stage.executeReactive(context, current)
                        .doOnNext(result -> {
                            Duration elapsed = Duration.between(stageStart, Instant.now());
                            String afterSnapshot = snapshotter.snapshot(result);
                            trail.add(new StageExecutionRecord(
                                    stage.name(), stageStart, elapsed,
                                    beforeSnapshot, afterSnapshot,
                                    null, context.isHalted(),
                                    context.getHaltReason(), context.getHaltReasonCode(),
                                    context.getHaltCode()));
                        })
                        .onErrorMap(PluginHaltException.class, ph -> translateHalt(stage, ph))
                        .onErrorResume(PipelineHaltException.class, halt -> {
                            LOG.warn("Pipeline halted by stage '{}': errorCode={}, haltCode={}, message={}",
                                    halt.getStageName(), halt.getErrorCode(), halt.getHaltCode(), halt.getMessage());
                            context.halt(halt.getStageName(), halt.getErrorCode(), halt.getHaltCode(), halt.getMessage());
                            PaymentLogSPI result = halt.getPartialResult() != null ? halt.getPartialResult() : current;
                            Duration elapsed = Duration.between(stageStart, Instant.now());
                            String afterSnapshot = snapshotter.snapshot(result);
                            trail.add(new StageExecutionRecord(
                                    stage.name(), stageStart, elapsed,
                                    beforeSnapshot, afterSnapshot,
                                    halt, true,
                                    context.getHaltReason(), context.getHaltReasonCode(),
                                    context.getHaltCode()));
                            return Mono.just(result);
                        })
                        .onErrorResume(ex -> {
                            LOG.error("Unhandled error in reactive pipeline stage '{}' for paymentType '{}': {}",
                                    stage.name(), context.getPaymentType(), ex.getMessage(), ex);
                            current.getDetails().setResponseCode("GN-01");
                            current.getDetails().setResponseDesc("Payment gateway processing error");
                            current.getDetails().setStatus("PENDING");
                            Duration elapsed = Duration.between(stageStart, Instant.now());
                            String afterSnapshot = snapshotter.snapshot(current);
                            trail.add(new StageExecutionRecord(
                                    stage.name(), stageStart, elapsed,
                                    beforeSnapshot, afterSnapshot,
                                    ex, false, null, null, null));
                            context.halt(stage.name(), "GN-01", ex.getMessage());
                            return Mono.just(current);
                        });
            });
        }

        return chain
                .flatMap(result -> {
                    // Compensation pipeline for reactive path
                    PaymentLogSPI compensated = executeCompensationIfNeeded(context, result, trail);
                    notifyListeners(context, compensated, trail);
                    return Mono.just(compensated);
                })
                .onErrorResume(ex -> {
                    LOG.error("Unhandled error in reactive pipeline for paymentType '{}': {}",
                            context.getPaymentType(), ex.getMessage(), ex);
                    request.getDetails().setResponseCode("GN-01");
                    request.getDetails().setResponseDesc("Payment gateway processing error");
                    request.getDetails().setStatus("PENDING");
                    notifyListeners(context, request, trail);
                    return Mono.just(request);
                });
    }

    /**
     * Appends a {@code skipped} trail record for every stage the halt walked past.
     *
     * <p>The executor stops at the first halt, so without this a compensation configured with six
     * stages that halts on stage two leaves four stages with no audit record at all — and the trail
     * is then indistinguishable from one where those stages were never configured. All the skips
     * share one timestamp: they are one event, and they sort as a block directly after the stage
     * that halted.
     */
    private void recordSkipped(List<StageExecutionRecord> trail, List<PipelineStage> remaining,
                               PipelineContext context, PaymentLogSPI current) {
        if (remaining.isEmpty()) {
            return;
        }
        Instant at = Instant.now();
        String snapshot = snapshotter.snapshot(current);
        for (PipelineStage stage : remaining) {
            trail.add(StageExecutionRecord.skipped(stage.name(), at, snapshot,
                    context.getHaltReason(), context.getHaltReasonCode(), context.getHaltCode()));
        }
    }

    /**
     * Normalises a halt signal into the Core exception the executor acts on.
     *
     * <p>A plugin handler cannot construct a {@link PipelineHaltException} — that type lives in
     * Core, and plugins compile against the SPI alone — so it throws
     * {@link PluginHaltException} and this fills in the one thing it has no way to know: which
     * stage slot it was running as. For a delegated stage that is the wrapper's display name
     * (e.g. {@code out-payment-handler>MPESA}), which is what the audit trail should record.
     *
     * <p>A {@link PipelineHaltException} thrown by a Core stage passes through untouched.
     */
    private static PipelineHaltException translateHalt(PipelineStage stage, Throwable e) {
        if (e instanceof PipelineHaltException halt) {
            return halt;
        }
        PluginHaltException ph = (PluginHaltException) e;
        return new PipelineHaltException(stage.name(), ph.getErrorCode(), ph.getHaltCode(),
                ph.getMessage(), ph.getPartialResult());
    }

    /**
     * Checks whether a compensation pipeline should be triggered and executes it synchronously.
     * Compensation pipelines can chain: if a compensation stage halts with its own halt code
     * and a matching compensation pipeline exists, it will be triggered recursively up to
     * the configured maximum depth.
     *
     * <p>The lookup is keyed on {@link #configuredPhase(String)} rather than the running phase, so a
     * halt raised <em>inside</em> a compensation resolves against the same {@code compensation:} map
     * the operator wrote for the phase. A compensation that halts with its own trigger code is
     * refused outright — see the guard below.
     */
    private PaymentLogSPI executeCompensationIfNeeded(PipelineContext context, PaymentLogSPI current,
                                                      List<StageExecutionRecord> trail) {
        if (!context.isHalted()) {
            return current;
        }

        String haltCode = context.getHaltCode();
        if (haltCode == null || haltCode.isEmpty()) {
            return current;
        }

        // A compensation that halts with the very code that triggered it would re-run the identical
        // stage list until the depth limit stopped it, and nothing about the second attempt differs.
        if (haltCode.equals(context.getAttribute("original.haltCode"))) {
            LOG.warn("Compensation for haltCode='{}' halted with the same code (paymentType='{}', phase='{}') " +
                            "— not re-entering.",
                    haltCode, context.getPaymentType(), context.getPipelinePhase());
            return current;
        }

        int maxDepth = registry.getMaxCompensationDepth();
        if (context.getCompensationDepth() >= maxDepth) {
            LOG.warn("Compensation depth limit reached ({}) for paymentType='{}', phase='{}', haltCode='{}'. " +
                            "Skipping further compensation.",
                    maxDepth, context.getPaymentType(), context.getPipelinePhase(), haltCode);
            return current;
        }

        String configuredPhase = configuredPhase(context.getPipelinePhase());
        List<PipelineStage> compStages = registry.getCompensationPipeline(
                context.getDirection(), context.getPaymentType(), configuredPhase, haltCode);

        if (compStages == null || compStages.isEmpty()) {
            LOG.debug("No compensation pipeline configured for direction='{}', paymentType='{}', phase='{}', haltCode='{}'",
                    context.getDirection(), context.getPaymentType(), configuredPhase, haltCode);
            return current;
        }

        LOG.info("Triggering compensation pipeline for haltCode='{}' (depth={}) — {} stages: {}",
                haltCode, context.getCompensationDepth() + 1, compStages.size(),
                compStages.stream().map(PipelineStage::name).toList());

        String compensationPhase = context.getPipelinePhase() + COMPENSATION_PHASE_MARKER + haltCode;
        PipelineContext compCtx = context.deriveCompensation(compensationPhase);
        // Store the original halt info for compensation stages to inspect
        compCtx.setAttribute("original.haltCode", haltCode);
        compCtx.setAttribute("original.haltReasonCode", context.getHaltReasonCode());
        compCtx.setAttribute("original.haltReason", context.getHaltReason());
        compCtx.setAttribute("original.haltedByStage", context.getHaltedByStage());

        return execute(compStages, compCtx, current);
    }

    /**
     * The phase a compensation pipeline is <em>configured</em> under.
     *
     * <p>A compensation context carries a derived phase — {@code in-payment-deferred-flow:compensation:
     * rndps-payment-onus} — so the audit trail can tell the nesting levels apart. Configuration,
     * though, is keyed on the phase the operator actually wrote. Resolving a nested halt against the
     * derived name can therefore never match a configured entry, and the nested compensation silently
     * does nothing; stripping back to the root phase is what makes nesting reachable at all.
     *
     * <p>Consequence: every halt code in a phase's {@code compensation:} map is reachable from any
     * depth within that phase. That is what lets a pipeline which halts to <em>branch</em> still
     * compensate the failure its branch runs into. {@code max-compensation-depth} remains the
     * recursion bound.
     */
    private static String configuredPhase(String pipelinePhase) {
        if (pipelinePhase == null) {
            return null;
        }
        int idx = pipelinePhase.indexOf(COMPENSATION_PHASE_MARKER);
        return idx < 0 ? pipelinePhase : pipelinePhase.substring(0, idx);
    }

    private void notifyListeners(PipelineContext context, PaymentLogSPI finalResult, List<StageExecutionRecord> trail) {
        for (PipelineExecutionListener listener : listeners) {
            try {
                listener.onPipelineComplete(context, finalResult, trail);
            } catch (Exception e) {
                LOG.error("Pipeline listener '{}' failed for paymentType='{}', phase='{}': {}",
                        listener.getClass().getSimpleName(), context.getPaymentType(),
                        context.getPipelinePhase(), e.getMessage(), e);
            }
        }
    }
}