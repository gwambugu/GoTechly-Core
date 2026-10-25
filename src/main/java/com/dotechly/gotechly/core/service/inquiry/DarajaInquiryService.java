package com.dotechly.gotechly.core.service.inquiry;


import com.dotechly.gotechly.core.domain.DarajaInquiry;
import com.dotechly.gotechly.core.pipeline.stages.callback.CallbackHttpClient;
import com.dotechly.gotechly.core.repository.DarajaInquiryRepository;
import com.dotechly.gotechly.spi.service.outward.inquiry.InquiryAck;
import com.dotechly.gotechly.spi.service.outward.inquiry.InquiryRequest;
import com.dotechly.gotechly.spi.service.outward.inquiry.InquiryResult;
import com.dotechly.gotechly.spi.service.outward.inquiry.InquiryType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;

/**
 * Persists every Daraja inquiry submission and the corresponding result webhook, and dispatches
 * the result to the caller's callback URL when one is supplied.
 * <p>
 * Persistence boundaries:
 * <ul>
 *   <li>{@link #recordSubmission} — called by the controller before the upstream POST; gives us
 *       a durable row even if the plugin call hangs.</li>
 *   <li>{@link #recordAck} — called after Daraja's immediate ack; stores ConversationID /
 *       OriginatorConversationID so the eventual webhook can be correlated.</li>
 *   <li>{@link #recordResult} — called when the result webhook arrives; updates final state
 *       and triggers callback fan-out.</li>
 * </ul>
 */
@Service
public class DarajaInquiryService {

    private static final Logger LOG = LoggerFactory.getLogger(DarajaInquiryService.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Duration CALLBACK_TIMEOUT = Duration.ofSeconds(15);

    private final DarajaInquiryRepository repository;
    private final CallbackHttpClient callbackHttpClient;

    public DarajaInquiryService(DarajaInquiryRepository repository,
                                CallbackHttpClient callbackHttpClient) {
        this.repository = repository;
        this.callbackHttpClient = callbackHttpClient;
    }

    @Transactional
    public DarajaInquiry recordSubmission(InquiryType type, InquiryRequest request) {
        DarajaInquiry row = repository.findByRequestId(request.requestId()).orElseGet(DarajaInquiry::new);
        row.setRequestId(request.requestId());
        row.setInquiryType(type.name());
        row.setSourceSystem(request.sourceSystem());
        row.setPartyA(request.partyA());
        row.setIdentifierType(request.identifierType());
        row.setCallbackUrl(request.callbackUrl());
        row.setCallbackStatus(request.callbackUrl() == null
                ? DarajaInquiry.CallbackStatus.NOT_APPLICABLE
                : DarajaInquiry.CallbackStatus.PENDING);
        row.setStatus(DarajaInquiry.Status.SUBMITTED);
        row.setRequestJson(safeJson(request));
        return repository.save(row);
    }

    @Transactional
    public void recordAck(String requestId, InquiryAck ack) {
        repository.findByRequestId(requestId).ifPresent(row -> {
            row.setConversationId(ack.conversationId());
            row.setOriginatorConversationId(ack.originatorConversationId());
            row.setAckResponseCode(ack.responseCode());
            row.setAckResponseDescription(ack.responseDescription());
            row.setResponseJson(safeJson(ack));
            // "0" is Daraja's accepted-for-processing marker; anything else is a fail-fast reject
            boolean accepted = "0".equals(ack.responseCode());
            row.setStatus(accepted ? DarajaInquiry.Status.ACKED : DarajaInquiry.Status.FAILED);
            repository.save(row);
        });
    }

    @Transactional
    public void recordSubmissionFailure(String requestId, String reason) {
        repository.findByRequestId(requestId).ifPresent(row -> {
            row.setStatus(DarajaInquiry.Status.FAILED);
            row.setAckResponseCode("EXCEPTION");
            row.setAckResponseDescription(truncate(reason, 255));
            repository.save(row);
        });
    }

    /**
     * Persist the parsed result and trigger callback dispatch (fire-and-forget — failures
     * only update the row's callback_status; they don't fail the webhook response to Daraja,
     * which would cause Daraja to retry the whole result unnecessarily).
     */
    @Transactional
    public Optional<DarajaInquiry> recordResult(InquiryType type, InquiryResult result) {
        Optional<DarajaInquiry> rowOpt = lookupForResult(result);
        if (rowOpt.isEmpty()) {
            LOG.warn("Daraja {} result has no correlating row: originatorConversationId={} requestId={}",
                    type, result.originatorConversationId(), result.requestId());
            return Optional.empty();
        }
        DarajaInquiry row = rowOpt.get();
        row.setResultCode(result.resultCode());
        row.setResultDescription(truncate(result.resultDescription(), 500));
        row.setResultJson(result.rawJson());
        boolean ok = "0".equals(result.resultCode());
        row.setStatus(ok ? DarajaInquiry.Status.COMPLETED : DarajaInquiry.Status.FAILED);
        DarajaInquiry saved = repository.save(row);

        if (saved.getCallbackUrl() != null
                && saved.getCallbackStatus() == DarajaInquiry.CallbackStatus.PENDING) {
            dispatchCallback(saved);
        }
        return Optional.of(saved);
    }

    public Optional<DarajaInquiry> findByRequestId(String requestId) {
        return repository.findByRequestId(requestId);
    }

    // ---- internals ----------------------------------------------------------

    private Optional<DarajaInquiry> lookupForResult(InquiryResult result) {
        if (result.originatorConversationId() != null) {
            Optional<DarajaInquiry> byOcid =
                    repository.findByOriginatorConversationId(result.originatorConversationId());
            if (byOcid.isPresent()) return byOcid;
        }
        // Fallback: the plugin may have surfaced the caller's requestId in the result if it
        // could resolve it from the Daraja payload directly (some Safaricom inquiry shapes echo
        // an arbitrary correlation field rather than OriginatorConversationID).
        if (result.requestId() != null) {
            return repository.findByRequestId(result.requestId());
        }
        return Optional.empty();
    }

    /**
     * Fire-and-forget callback to the caller. Runs on Reactor's boundedElastic scheduler since
     * we're inside a JPA @Transactional method and don't want to block on the HTTP call. The
     * subsequent persist of callback_status runs in its own transaction.
     */
    private void dispatchCallback(DarajaInquiry row) {
        String url = row.getCallbackUrl();
        String body = row.getResultJson() == null ? "{}" : row.getResultJson();
        Long rowId = row.getId();
        int attempt = row.getCallbackAttempts() + 1;

        callbackHttpClient.postJson(url, body, null, CALLBACK_TIMEOUT)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(resp -> {
                    boolean ok = resp.statusCode() >= 200 && resp.statusCode() < 300;
                    LOG.info("Daraja inquiry callback to {} returned HTTP {} (attempt={})",
                            url, resp.statusCode(), attempt);
                    updateCallbackStatus(rowId, attempt,
                            ok ? DarajaInquiry.CallbackStatus.DELIVERED
                               : DarajaInquiry.CallbackStatus.FAILED);
                })
                .doOnError(ex -> {
                    LOG.error("Daraja inquiry callback to {} failed (attempt={}): {}",
                            url, attempt, ex.getMessage());
                    updateCallbackStatus(rowId, attempt, DarajaInquiry.CallbackStatus.FAILED);
                })
                .subscribe();
    }

    // No @Transactional: called from a Reactor lambda where `this` is the raw bean (not the
    // proxy), so the annotation would be ignored. JpaRepository.save() is already wrapped in
    // its own transaction by Spring Data, which is sufficient for this single-row update.
    void updateCallbackStatus(Long rowId, int attempts, DarajaInquiry.CallbackStatus status) {
        repository.findById(rowId).ifPresent(r -> {
            r.setCallbackAttempts(attempts);
            r.setCallbackStatus(status);
            repository.save(r);
        });
    }

    private static String safeJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
