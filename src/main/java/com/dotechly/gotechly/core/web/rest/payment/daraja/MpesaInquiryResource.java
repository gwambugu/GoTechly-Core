package com.dotechly.gotechly.core.web.rest.payment.daraja;


import com.dotechly.gotechly.core.config.outward.DarajaInquiryHandlerRegistry;
import com.dotechly.gotechly.core.domain.DarajaInquiry;
import com.dotechly.gotechly.core.service.inquiry.DarajaInquiryService;
import com.dotechly.gotechly.spi.service.outward.inquiry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Caller-facing REST surface for Safaricom Daraja inquiries (balance, status, identity,
 * agent validate, sfcverify) plus the result-webhook endpoint Daraja itself POSTs to.
 *
 * <h3>Routes</h3>
 * <ul>
 *   <li>{@code POST /payment/api/v1/mpesa/inquiry/{type}} — submit an inquiry. Returns 202 with
 *       the immediate Daraja ack ({@code ConversationID}, {@code OriginatorConversationID}).</li>
 *   <li>{@code POST /payment/api/v1/mpesa/inquiry/callback/{type}} — Daraja's result-webhook
 *       sink. Permitted by Spring Security and isolated to the dedicated daraja-callback
 *       multi-port listener (firewall allowlist enforces caller identity).</li>
 *   <li>{@code GET /payment/api/v1/mpesa/inquiry/{requestId}} — polling read for callers that
 *       can't accept a callback.</li>
 * </ul>
 */
@RestController
@RequestMapping("/payment/api/v1/mpesa/inquiry")
public class MpesaInquiryResource {

    private static final Logger LOG = LoggerFactory.getLogger(MpesaInquiryResource.class);

    private static final String PAYMENT_TYPE = "HAKIKISHA";

    private final DarajaInquiryHandlerRegistry handlerRegistry;
    private final DarajaInquiryService inquiryService;

    public MpesaInquiryResource(DarajaInquiryHandlerRegistry handlerRegistry,
                                DarajaInquiryService inquiryService) {
        this.handlerRegistry = handlerRegistry;
        this.inquiryService = inquiryService;
    }

    /**
     * Submit an inquiry to Daraja. Persists upfront so we have a row even if the upstream POST
     * hangs; updates with the ack (or failure marker) before returning to the caller.
     */
    @PostMapping(value = "/{type}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<InquiryAck>> submit(
            @PathVariable("type") String typeRaw,
            @RequestBody InquiryRequest body) {

        InquiryType type = parseType(typeRaw);
        InquiryRequest request = ensureRequestId(body);

        DarajaInquiryHandler handler = handlerRegistry.findByPaymentType(PAYMENT_TYPE)
                .orElseThrow(() -> new IllegalStateException(
                        "No Daraja inquiry handler registered for paymentType=" + PAYMENT_TYPE +
                                ". Is the HAKIKISHA plugin JAR loaded?"));

        inquiryService.recordSubmission(type, request);

        return handler.inquire(type, request)
                .doOnNext(ack -> inquiryService.recordAck(request.requestId(), ack))
                .map(ack -> ResponseEntity.accepted().body(ack))
                .onErrorResume(DarajaInquiryException.class, ex -> {
                    LOG.warn("Daraja {} inquiry rejected for requestId={}: {}",
                            type, request.requestId(), ex.getMessage());
                    inquiryService.recordSubmissionFailure(request.requestId(), ex.getMessage());
                    HttpStatus status = mapErrorCode(ex.getErrorCode());
                    InquiryAck failed = new InquiryAck(request.requestId(), null, null,
                            ex.getErrorCode(), ex.getMessage());
                    return Mono.just(ResponseEntity.status(status).body(failed));
                })
                .onErrorResume(ex -> {
                    LOG.error("Unexpected Daraja {} inquiry failure for requestId={}",
                            type, request.requestId(), ex);
                    inquiryService.recordSubmissionFailure(request.requestId(), ex.getMessage());
                    InquiryAck failed = new InquiryAck(request.requestId(), null, null,
                            "INTERNAL_ERROR", ex.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(failed));
                });
    }

    /**
     * Daraja's async result webhook. Daraja retries on non-2xx so we always return 200 OK after
     * persisting (or attempting to persist) the raw body — failures are logged and the row is
     * marked FAILED, but Daraja doesn't need to retry transport-level.
     */
    @PostMapping(value = "/callback/{type}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> resultWebhook(
            @PathVariable("type") String typeRaw,
            @RequestBody String rawJson) {

        InquiryType type = parseType(typeRaw);
        DarajaInquiryHandler handler = handlerRegistry.findByPaymentType(PAYMENT_TYPE)
                .orElseThrow(() -> new IllegalStateException(
                        "No Daraja inquiry handler registered for paymentType=" + PAYMENT_TYPE));

        return handler.handleResult(type, rawJson)
                .map(parsed -> {
                    inquiryService.recordResult(type, parsed);
                    return ResponseEntity.ok("{\"ResultCode\":0,\"ResultDesc\":\"Accepted\"}");
                })
                .onErrorResume(ex -> {
                    LOG.error("Daraja {} result webhook parse failed; raw body persisted untouched: {}",
                            type, ex.getMessage(), ex);
                    return Mono.just(ResponseEntity.ok(
                            "{\"ResultCode\":0,\"ResultDesc\":\"Accepted (parse error logged)\"}"));
                });
    }

    /**
     * Polling read for callers that don't accept callbacks. Returns 404 until the submission
     * row is written.
     */
    @GetMapping(value = "/{requestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DarajaInquiry> get(@PathVariable("requestId") String requestId) {
        return inquiryService.findByRequestId(requestId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---- internals ----------------------------------------------------------

    private static InquiryType parseType(String raw) {
        try {
            return InquiryType.valueOf(raw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown inquiry type: " + raw +
                    " (valid: BALANCE, STATUS, IDENTITY, AGENT_VALIDATE, SFC_VERIFY)");
        }
    }

    private static InquiryRequest ensureRequestId(InquiryRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Inquiry request body is required");
        }
        if (req.requestId() != null && !req.requestId().isBlank()) {
            return req;
        }
        return new InquiryRequest(
                UUID.randomUUID().toString(),
                req.sourceSystem(), req.callbackUrl(),
                req.partyA(), req.identifierType(), req.additionalParams());
    }

    private static HttpStatus mapErrorCode(String errorCode) {
        if (errorCode == null) return HttpStatus.BAD_REQUEST;
        if (errorCode.startsWith("DARAJA-CONFIG")) return HttpStatus.SERVICE_UNAVAILABLE;
        if (errorCode.startsWith("DARAJA-HTTP-")) {
            try {
                int code = Integer.parseInt(errorCode.substring("DARAJA-HTTP-".length()));
                HttpStatus mapped = HttpStatus.resolve(code);
                return mapped != null ? mapped : HttpStatus.BAD_GATEWAY;
            } catch (NumberFormatException ignored) {
                return HttpStatus.BAD_GATEWAY;
            }
        }
        return HttpStatus.BAD_GATEWAY;
    }
}
