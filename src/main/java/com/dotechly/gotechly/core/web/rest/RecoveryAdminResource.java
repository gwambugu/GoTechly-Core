package com.dotechly.gotechly.core.web.rest;

import com.dotechly.gotechly.core.domain.DeferredRecovery;
import com.dotechly.gotechly.core.recovery.DeferredRecoveryQueue;
import com.dotechly.gotechly.core.recovery.DeferredRecoveryService;
import com.dotechly.gotechly.core.recovery.RecoveryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Operator surface for the reconciler, under the portal API prefix so the Gateway routes it with the
 * rest of {@code /api/**} and no new route or port is needed.
 *
 * <p>The endpoint that matters operationally is {@code GET /metrics}: {@code needsAttention} is the
 * number of transactions where core banking does not match what a scheme was told, and it is the
 * only number here that should ever page someone. {@code queue.deferredOffers} rising is the earlier,
 * softer signal that core banking has been failing long enough to fill the working set.
 */
@RestController
@RequestMapping("/api/v1/admin/recovery")
public class RecoveryAdminResource {

    private static final Logger LOG = LoggerFactory.getLogger(RecoveryAdminResource.class);

    private final DeferredRecoveryService service;
    private final DeferredRecoveryQueue queue;

    public RecoveryAdminResource(DeferredRecoveryService service, DeferredRecoveryQueue queue) {
        this.service = service;
        this.queue = queue;
    }

    public record RecoveryMetrics(Map<RecoveryModel.RecoveryState, Long> states,
                                  long needsAttention,
                                  DeferredRecoveryQueue.QueueMetrics queue,
                                  Instant asOf) {
    }

    /** Queue depth, retry pressure and — the one to alert on — the escalation count. */
    @GetMapping("/metrics")
    public ResponseEntity<RecoveryMetrics> metrics() {
        Map<RecoveryModel.RecoveryState, Long> counts = service.counts();
        return ResponseEntity.ok(new RecoveryMetrics(
                counts,
                counts.getOrDefault(RecoveryModel.RecoveryState.NEEDS_ATTENTION, 0L),
                queue.metrics(),
                Instant.now()));
    }

    /** Paged listing, optionally filtered by state. Newest first. */
    @GetMapping
    public ResponseEntity<List<DeferredRecovery>> list(
            @RequestParam(required = false) String state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        RecoveryModel.RecoveryState parsed = parseState(state);
        Page<DeferredRecovery> result = service.list(parsed,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 500),
                        Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result.getContent());
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<DeferredRecovery> get(@PathVariable String requestId) {
        return service.find(requestId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Puts an escalated or cancelled transaction back in the queue — the loop an operator closes
     * after core banking is fixed. Resets the attempt budget and the deadline, because the operator
     * is asserting that the thing which was failing no longer is.
     */
    @PostMapping("/{requestId}/requeue")
    public ResponseEntity<DeferredRecovery> requeue(@PathVariable String requestId) {
        LOG.warn("[RECOVERY] operator requeue requested for requestId '{}'", requestId);
        return service.requeue(requestId)
                .map(row -> {
                    queue.offer(row);
                    return ResponseEntity.ok(row);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Stops trying. For when the inconsistency is being settled outside the system — a manual Finacle
     * entry, or a scheme-side adjustment. The row stays as the record of what happened.
     */
    @PostMapping("/{requestId}/cancel")
    public ResponseEntity<DeferredRecovery> cancel(@PathVariable String requestId,
                                                   @RequestParam(defaultValue = "Cancelled by operator") String reason) {
        return service.cancel(requestId, reason)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static RecoveryModel.RecoveryState parseState(String state) {
        if (state == null || state.isBlank() || "ALL".equalsIgnoreCase(state)) {
            return null;
        }
        try {
            return RecoveryModel.RecoveryState.valueOf(state.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
