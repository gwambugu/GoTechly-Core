package com.dotechly.gotechly.core.pipeline.audit;

import com.dotechly.gotechly.core.domain.AuditLog;
import com.dotechly.gotechly.core.pipeline.PipelineContext;
import com.dotechly.gotechly.core.pipeline.PipelineHaltException;
import com.dotechly.gotechly.core.repository.AuditLogRepository;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Pipeline execution listener that persists a full audit trail to the database.
 *
 * <p>For each pipeline execution, this listener:
 * <ol>
 *   <li>Creates one {@link AuditLog} record per stage that executed (recording field-level diffs)</li>
 *   <li>Creates a {@code PIPELINE_COMPLETED} summary record with the full initial and final state</li>
 *   <li>Batch-saves all records to the database</li>
 * </ol>
 *
 * <p>Since this runs after the pipeline completes (via the executor's finally block),
 * it <strong>always</strong> fires — even on halts and unhandled exceptions.
 * Persistence is async (dispatched to the {@code auditExecutor} thread pool)
 * so it never adds latency to the payment response.</p>
 */
@Component
public class AuditPipelineListener implements PipelineExecutionListener {

    private static final Logger LOG = LoggerFactory.getLogger(AuditPipelineListener.class);

    private static final String EVENT_STAGE_EXECUTED = "STAGE_EXECUTED";
    private static final String EVENT_STAGE_HALTED = "STAGE_HALTED";
    private static final String EVENT_STAGE_ERROR = "STAGE_ERROR";
    private static final String EVENT_STAGE_SKIPPED = "STAGE_SKIPPED";
    private static final String EVENT_PIPELINE_COMPLETED = "PIPELINE_COMPLETED";

    private static final String ACTOR_TYPE = "PIPELINE_STAGE";
    private static final String ENTITY_NAME = "PaymentLogSPI";

    /** Matches {@code AuditLog.phaseName} / {@code HistoryAuditLog.phaseName}. */
    private static final int PHASE_NAME_MAX = 100;
    private static final String PHASE_NAME_ELISION = "..";
    private static final String SERVICE_NAME = "GoTechly-Core";

    private final AuditLogRepository auditLogRepository;
    private final PaymentLogSnapshotter snapshotter;

    public AuditPipelineListener(AuditLogRepository auditLogRepository, PaymentLogSnapshotter snapshotter) {
        this.auditLogRepository = auditLogRepository;
        this.snapshotter = snapshotter;
    }

    @Async("auditExecutor")
    @Transactional
    @Override
    public void onPipelineComplete(PipelineContext context, PaymentLogSPI finalResult, List<StageExecutionRecord> trail) {
        try {
            List<AuditLog> records = new ArrayList<>();
            String requestId = extractRequestId(finalResult);

            for (StageExecutionRecord record : trail) {
                records.add(buildStageAuditRecord(record, context, requestId));
            }

            records.add(buildPipelineSummaryRecord(context, finalResult, trail, requestId));

            auditLogRepository.saveAll(records);

            LOG.debug("Persisted {} audit records for requestId='{}', phase='{}'",
                    records.size(), requestId, context.getPipelinePhase());
        } catch (Exception e) {
            LOG.error("Failed to persist audit trail for paymentType='{}', phase='{}': {}",
                    context.getPaymentType(), context.getPipelinePhase(), e.getMessage(), e);
        }
    }

    private AuditLog buildStageAuditRecord(StageExecutionRecord record, PipelineContext context, String requestId) {
        String eventType = resolveEventType(record);
        Map<String, PaymentLogSnapshotter.FieldChange> diff =
                snapshotter.computeDiff(record.beforeSnapshot(), record.afterSnapshot());

        Map<String, String> oldValues = new LinkedHashMap<>();
        Map<String, String> newValues = new LinkedHashMap<>();
        for (Map.Entry<String, PaymentLogSnapshotter.FieldChange> entry : diff.entrySet()) {
            oldValues.put(entry.getKey(), entry.getValue().oldValue());
            newValues.put(entry.getKey(), entry.getValue().newValue());
        }

        AuditLog audit = new AuditLog();
        audit.setEventType(eventType);
        audit.setEntityName(ENTITY_NAME);
        audit.setPhaseName(phaseLabel(context.getPipelinePhase(), record.stageName()));
        audit.setRequestId(requestId);
        audit.setActorId(record.stageName());
        audit.setActorType(ACTOR_TYPE);
        audit.setServiceName(SERVICE_NAME);
        // The moment the stage ran, not the moment this listener got round to building the row.
        // Persistence order is not execution order: this listener is @Async on a small pool, a
        // parent pipeline's trail is written only after its compensations' trails, and sibling
        // trails on different pool threads interleave their ids. createdAt is what the trail is
        // ordered by, so it has to carry the stage clock.
        audit.setCreatedAt(record.startedAt() != null ? record.startedAt() : Instant.now());

        if (!diff.isEmpty()) {
            audit.setChangedFields(snapshotter.changedFieldsToJson(diff.keySet()));
            audit.setOldValue(snapshotter.valuesToJson(oldValues));
            audit.setNewValue(snapshotter.valuesToJson(newValues));
        }

        if (record.error() != null) {
            String errorDetail = formatError(record);
            audit.setEnvironment(truncate(errorDetail, 20));
        }

        audit.setRole(formatDuration(record.elapsed()));
        audit.setSessionId(context.getDirection() + ":" + context.getPaymentType());

        return audit;
    }

    private AuditLog buildPipelineSummaryRecord(PipelineContext context, PaymentLogSPI finalResult,
                                                List<StageExecutionRecord> trail, String requestId) {
        String initialSnapshot = trail.isEmpty() ? "{}" : trail.get(0).beforeSnapshot();
        String finalSnapshot = snapshotter.snapshot(finalResult);

        Map<String, PaymentLogSnapshotter.FieldChange> totalDiff =
                snapshotter.computeDiff(initialSnapshot, finalSnapshot);

        Map<String, String> oldValues = new LinkedHashMap<>();
        Map<String, String> newValues = new LinkedHashMap<>();
        for (Map.Entry<String, PaymentLogSnapshotter.FieldChange> entry : totalDiff.entrySet()) {
            oldValues.put(entry.getKey(), entry.getValue().oldValue());
            newValues.put(entry.getKey(), entry.getValue().newValue());
        }

        AuditLog summary = new AuditLog();
        summary.setEventType(EVENT_PIPELINE_COMPLETED);
        summary.setEntityName(ENTITY_NAME);
        summary.setPhaseName(phaseLabel(context.getPipelinePhase(), null));
        summary.setRequestId(requestId);
        summary.setActorId("pipeline-executor");
        summary.setActorType("SYSTEM");
        summary.setServiceName(SERVICE_NAME);
        summary.setCreatedAt(Instant.now());

        if (!totalDiff.isEmpty()) {
            summary.setChangedFields(snapshotter.changedFieldsToJson(totalDiff.keySet()));
            summary.setOldValue(snapshotter.valuesToJson(oldValues));
            summary.setNewValue(snapshotter.valuesToJson(newValues));
        }

        Duration totalDuration = Duration.between(context.getStartedAt(), Instant.now());
        summary.setRole(formatDuration(totalDuration));

        summary.setSessionId(context.getDirection() + ":" + context.getPaymentType());

        if (context.isHalted()) {
            String haltInfo = "HALTED by " + context.getHaltedByStage()
                    + " [" + context.getHaltReasonCode() + "]"
                    + (context.getHaltCode() != null ? " haltCode=" + context.getHaltCode() : "")
                    + " " + context.getHaltReason();
            summary.setActorUsername(truncate(haltInfo, 100));
        } else {
            summary.setActorUsername(finalResult.getDetails() != null
                    ? truncate(finalResult.getDetails().getStatus(), 100) : "UNKNOWN");
        }

        summary.setIpAddress(extractSourceIp(finalResult));

        // Skipped stages are in the trail but were never run — counting them as executed would
        // report a halted six-stage compensation as "6 stages executed".
        long stagesExecuted = trail.stream().filter(r -> !r.skipped()).count();
        long stagesSkipped = trail.stream().filter(StageExecutionRecord::skipped).count();
        long stagesWithChanges = trail.stream()
                .filter(r -> !Objects.equals(r.beforeSnapshot(), r.afterSnapshot()))
                .count();
        long stagesWithErrors = trail.stream().filter(r -> r.error() != null).count();
        summary.setEndpoint(stagesExecuted + " stages executed, "
                + stagesWithChanges + " modified, " + stagesWithErrors + " errors"
                + (stagesSkipped > 0 ? ", " + stagesSkipped + " skipped" : ""));

        return summary;
    }

    private String resolveEventType(StageExecutionRecord record) {
        // Checked first: a skipped record also carries halted=true (the pipeline *was* halted when
        // the executor walked past it), so the halt branch below would otherwise claim it and every
        // skipped stage would read as the stage that did the halting.
        if (record.skipped()) {
            return EVENT_STAGE_SKIPPED;
        }
        if (record.error() instanceof PipelineHaltException || record.halted()) {
            return EVENT_STAGE_HALTED;
        }
        if (record.error() != null) {
            return EVENT_STAGE_ERROR;
        }
        return EVENT_STAGE_EXECUTED;
    }

    private String formatError(StageExecutionRecord record) {
        Throwable error = record.error();
        if (error instanceof PipelineHaltException halt) {
            return halt.getErrorCode();
        }
        String name = error.getClass().getSimpleName();
        return name.length() > 20 ? name.substring(0, 20) : name;
    }

    private String formatDuration(Duration duration) {
        if (duration == null) return null;
        return duration.toMillis() + "ms";
    }

    private String extractRequestId(PaymentLogSPI result) {
        if (result != null && result.getDetails() != null && result.getDetails().getRequestId() != null) {
            return truncate(result.getDetails().getRequestId(), 200);
        }
        return "unknown";
    }

    private String extractSourceIp(PaymentLogSPI result) {
        if (result != null && result.getDetails() != null) {
            return truncate(result.getDetails().getSourceIp(), 50);
        }
        return null;
    }

    /**
     * Composes the {@code phaseName} label so it always fits the column.
     *
     * <p>A nested compensation phase is long: {@code in-payment-deferred-flow:compensation:
     * rndps-payment-onus:compensation:cbc-payment-failed} is 87 of the 100 characters before a
     * stage name is appended. Bean Validation rejects the oversized insert at commit, and because
     * the whole trail is saved in one {@code saveAll}, that discards <em>every</em> record for the
     * phase — not just the one that overflowed. Audit is an observer; it must never lose a trail
     * over a label.
     *
     * <p>The elision falls on the middle of the phase. The head names the phase an operator filters
     * on, the tail names the halt code that got us here, and the stage suffix is what distinguishes
     * one record in the trail from the next — so the middle of a compensation chain is the only
     * part that can go.
     */
    private String phaseLabel(String pipelinePhase, String stageName) {
        String phase = (pipelinePhase == null) ? "" : pipelinePhase;
        String suffix = (stageName == null || stageName.isBlank()) ? "" : ":" + stageName;

        String label = phase + suffix;
        if (label.length() <= PHASE_NAME_MAX) {
            return label;
        }
        if (suffix.length() >= PHASE_NAME_MAX) {
            return truncate(suffix, PHASE_NAME_MAX);
        }

        int room = PHASE_NAME_MAX - suffix.length() - PHASE_NAME_ELISION.length();
        int headLen = room / 2;
        int tailLen = room - headLen;
        return phase.substring(0, headLen) + PHASE_NAME_ELISION
                + phase.substring(phase.length() - tailLen) + suffix;
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
