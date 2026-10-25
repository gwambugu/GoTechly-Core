package com.dotechly.gotechly.core.web.rest.payment.legacy.out.payment;

import com.dotechly.gotechly.core.config.inward.InPaymentProcessingHandlerRegistry;
import com.dotechly.gotechly.core.config.outward.OutPaymentProcessingHandlerRegistry;
import com.dotechly.gotechly.core.service.PaymentRendezvousImp;
import com.dotechly.gotechly.core.service.plugin.payment.ReactivePaymentProcessingService;
import com.dotechly.gotechly.spi.service.LegacyRequestAdapter;
import com.dotechly.gotechly.spi.service.dto.payment.LegacyResponse;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.outward.OutPaymentRequestProcessingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payment/api/v1/legacy/out/pay")
public class LegacyOutPaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyOutPaymentResource.class);

    private final OutPaymentProcessingHandlerRegistry  outPaymentRegistry;
    private final ReactivePaymentProcessingService  reactivePaymentService;
    private final PaymentRendezvousImp paymentRendezvous;

    public LegacyOutPaymentResource(OutPaymentProcessingHandlerRegistry  outPaymentRegistry,
                                 InPaymentProcessingHandlerRegistry  inPaymentRegistry,
                                 ReactivePaymentProcessingService  reactivePaymentService,
                                    PaymentRendezvousImp paymentRendezvous) {
        this.outPaymentRegistry = outPaymentRegistry;
        this.reactivePaymentService = reactivePaymentService;
        this.paymentRendezvous = paymentRendezvous;
    }

    @PostMapping(value = "/{paymentType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> paymentOutwardJson(@PathVariable("paymentType") String paymentType,
                                                       @RequestBody Object legacyPayload) {
        LOG.info("paymentOutwardJson {} {}", paymentType, legacyPayload);
        return outwardDispatch(paymentType, legacyPayload, MediaType.APPLICATION_JSON_VALUE);
    }

    @PostMapping(value = "/{paymentType}",
                 consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public Mono<ResponseEntity<?>> paymentOutwardXml(@PathVariable("paymentType") String paymentType,
                                                      @RequestBody String legacyPayload,
                                                      @RequestHeader(value = "Content-Type",
                                                              defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        LOG.info("paymentAsyncOutwardXml {} {}", paymentType, legacyPayload);
        return outwardDispatch(paymentType, legacyPayload, contentType);
    }

    @PostMapping(value = "/async/{paymentType}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public String paymentAsyncOutwardXml(@PathVariable("paymentType") String paymentType,
                                                                  @RequestBody String legacyPayload) {
        LOG.info("paymentAsyncOutwardXml {} {}", paymentType, legacyPayload);
        return outwardAsyncDispatch(paymentType, legacyPayload);
    }

    @PostMapping(value = "/sync-over-async/{paymentType}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public Mono<ResponseEntity<?>> paymentSyncOverAsyncOutwardXml(@PathVariable("paymentType") String paymentType,
                                                                  @RequestBody String legacyPayload,
                                                                  @RequestHeader(value = "Content-Type",
                                                                          defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return outwardSyncOverAsyncDispatch(paymentType, legacyPayload,contentType);
    }

    private Mono<ResponseEntity<?>> outwardDispatch(String paymentType, Object legacyPayload, String inboundContentType) {
        LOG.debug("Legacy outward payment request for paymentType '{}' ({})", paymentType, inboundContentType);
        OutPaymentRequestProcessingHandler handler;
        try {
            handler = outPaymentRegistry.getHandler(paymentType);
        } catch (IllegalArgumentException e) {
            LOG.error("IllegalArgumentException {}",e.getMessage());
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return dispatch(paymentType, handler, legacyPayload, "outward", inboundContentType);
    }

    private String outwardAsyncDispatch(String paymentType, Object legacyPayload) {
        LOG.debug("Legacy outward payment request for paymentType '{}'", paymentType);
        OutPaymentRequestProcessingHandler handler;
        try {
            handler = outPaymentRegistry.getHandler(paymentType);
        } catch (IllegalArgumentException e) {
            LOG.error("IllegalArgumentException {}",e.getMessage());
            return "BAD_REQUEST";
        }
        return dispatchAsync(paymentType, handler, legacyPayload, "outward");
    }
    private Mono<ResponseEntity<?>> outwardSyncOverAsyncDispatch(String paymentType, Object legacyPayload, String inboundContentType) {
        LOG.debug("Legacy outward payment request for paymentType '{}' ({})", paymentType, inboundContentType);
        OutPaymentRequestProcessingHandler handler;
        try {
            handler = outPaymentRegistry.getHandler(paymentType);
        } catch (IllegalArgumentException e) {
            LOG.error("IllegalArgumentException {}",e.getMessage());
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return dispatchSyncOverAsync(paymentType, handler, legacyPayload, "outward", inboundContentType);
    }

    private Mono<ResponseEntity<?>> dispatch(String paymentType, Object handler, Object legacyPayload,
                                             String direction, String inboundContentType) {
        if (!(handler instanceof LegacyRequestAdapter adapter)) {
            LOG.warn("{} handler for paymentType '{}' does not implement LegacyRequestAdapter",
                    direction, paymentType);
            return Mono.just(ResponseEntity.status(501).build());
        }

        PaymentLogSPI converted;
        try {
            converted = adapter.legacyToPaymentLogSPI(legacyPayload);
        } catch (IllegalArgumentException e) {
            LOG.warn("Legacy payload rejected by {} {} adapter: {}", direction, paymentType, e.getMessage());
            return Mono.just(ResponseEntity.badRequest().build());
        }

        if (converted == null || converted.getDetails() == null
                || converted.getDetails().getPaymentType() == null) {
            LOG.warn("Legacy adapter for {} '{}' returned an invalid PaymentLogSPI", direction, paymentType);
            return Mono.just(ResponseEntity.status(500).build());
        }

        return reactivePaymentService.paymentProcessingRequest(converted, direction)
                .map(result -> renderLegacyResponse(adapter, result, inboundContentType, paymentType));
    }

    private String dispatchAsync(String paymentType, Object handler,
                                                         Object legacyPayload, String direction) {
        if (!(handler instanceof LegacyRequestAdapter adapter)) {
            LOG.warn("{} handler for paymentType '{}' does not implement LegacyRequestAdapter",
                    direction, paymentType);
            return "REJECTED UNSUPPORTED";
        }

        PaymentLogSPI converted;
        try {
            converted = adapter.legacyToPaymentLogSPI(legacyPayload);
        } catch (IllegalArgumentException e) {
            LOG.warn("Legacy payload rejected by {} {} adapter: {}", direction, paymentType, e.getMessage());
            return "REJECTED";
        }

        if (converted == null || converted.getDetails() == null
                || converted.getDetails().getPaymentType() == null) {
            LOG.warn("Legacy adapter for {} '{}' returned an invalid PaymentLogSPI", direction, paymentType);
            return "REJECTED";
        }

        return reactivePaymentService.paymentProcessingRequestAsync(converted, direction);
    }

    private Mono<ResponseEntity<?>> dispatchSyncOverAsync(String paymentType, Object handler, Object legacyPayload,
                                             String direction, String inboundContentType) {
        if (!(handler instanceof LegacyRequestAdapter adapter)) {
            LOG.warn("{} handler for paymentType '{}' does not implement LegacyRequestAdapter",
                    direction, paymentType);
            return Mono.just(ResponseEntity.status(501).build());
        }

        PaymentLogSPI converted;
        try {
            converted = adapter.legacyToPaymentLogSPI(legacyPayload);
        } catch (IllegalArgumentException e) {
            LOG.warn("Legacy payload rejected by {} {} adapter: {}", direction, paymentType, e.getMessage());
            return Mono.just(ResponseEntity.badRequest().build());
        }

        if (converted == null || converted.getDetails() == null
                || converted.getDetails().getPaymentType() == null) {
            LOG.warn("Legacy adapter for {} '{}' returned an invalid PaymentLogSPI", direction, paymentType);
            return Mono.just(ResponseEntity.status(500).build());
        }


        return reactivePaymentService.paymentProcessingRequest(converted, "outward")
                .then(Mono.defer(() -> {
                    LOG.info("Sync over async waiting for result: {}", converted);
                    return paymentRendezvous.park(converted.getDetails().getRequestId());
                })).map(result -> renderLegacyResponse(adapter, result, inboundContentType, paymentType));
    }

    /**
     * Ask the inbound handler's adapter to render the pipeline outcome. If it returns
     * null (opted out) or throws, fall back to JSON PaymentLogSPI so a formatting
     * failure never hides the actual pipeline result.
     * Uses the original adapter reference so IMT composites render the IMT response
     * shape regardless of the rewritten downstream payment type.
     */
    private ResponseEntity<?> renderLegacyResponse(LegacyRequestAdapter adapter, PaymentLogSPI result,
                                                   String inboundContentType, String paymentType) {
        try {
            LegacyResponse legacy = adapter.toLegacyResponse(result, inboundContentType);
            if (legacy != null && legacy.body() != null) {
                String ct = legacy.contentType() != null ? legacy.contentType() : MediaType.APPLICATION_JSON_VALUE;
                return ResponseEntity.ok().header("Content-Type", ct).body(legacy.body());
            }
        } catch (Exception e) {
            LOG.warn("Legacy response rendering failed for {}; falling back to JSON: {}",
                    paymentType, e.getMessage(), e);
        }
        return ResponseEntity.ok(result);
    }
}
