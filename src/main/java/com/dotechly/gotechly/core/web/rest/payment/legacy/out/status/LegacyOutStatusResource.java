package com.dotechly.gotechly.core.web.rest.payment.legacy.out.status;

import com.dotechly.gotechly.core.config.inward.InValidationHandlerRegistry;
import com.dotechly.gotechly.core.config.outward.OutStatusProcessingHandlerRegistry;
import com.dotechly.gotechly.core.config.outward.OutValidationHandlerRegistry;
import com.dotechly.gotechly.core.service.PaymentRendezvousImp;
import com.dotechly.gotechly.core.service.plugin.status.ReactiveStatusService;
import com.dotechly.gotechly.core.service.plugin.validation.ReactiveValidationService;
import com.dotechly.gotechly.spi.service.LegacyRequestAdapter;
import com.dotechly.gotechly.spi.service.dto.payment.LegacyResponse;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.outward.OutStatusRequestProcessingHandler;
import com.dotechly.gotechly.spi.service.outward.OutValidationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/payment/api/v1/legacy/out/status")
public class LegacyOutStatusResource {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyOutStatusResource.class);

    private final OutStatusProcessingHandlerRegistry  outStatusHandlerRegistry;
    private final ReactiveStatusService reactiveStatusService;
    private final PaymentRendezvousImp paymentRendezvous;

    public LegacyOutStatusResource(OutStatusProcessingHandlerRegistry outStatusHandlerRegistry,
                                   ReactiveStatusService reactiveStatusService,
                                   PaymentRendezvousImp paymentRendezvous) {
        this.outStatusHandlerRegistry = outStatusHandlerRegistry;
        this.reactiveStatusService = reactiveStatusService;
        this.paymentRendezvous = paymentRendezvous;
    }

    @PostMapping(value = "/{paymentType}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<?>> validateOutwardJson(@PathVariable("paymentType") String paymentType,
                                                       @RequestBody Object legacyPayload) {
        return outwardDispatch(paymentType, legacyPayload, MediaType.APPLICATION_JSON_VALUE);
    }

    @PostMapping(value = "/{paymentType}",
                 consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public Mono<ResponseEntity<?>> validateOutwardXml(@PathVariable("paymentType") String paymentType,
                                                      @RequestBody String legacyPayload,
                                                      @RequestHeader(value = "Content-Type",
                                                              defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return outwardDispatch(paymentType, legacyPayload, contentType);
    }

    @PostMapping(value = "/async/{paymentType}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public String validateAsyncOutwardXml(@PathVariable("paymentType") String paymentType,
                                                                  @RequestBody String legacyPayload) {
        return outwardAsyncDispatch(paymentType, legacyPayload);
    }

    @PostMapping(value = "/sync-over-async/{paymentType}",
            consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public Mono<ResponseEntity<?>> validateSycnOverAsyncOutwardXml(@PathVariable("paymentType") String paymentType,
                                                      @RequestBody String legacyPayload,
                                                      @RequestHeader(value = "Content-Type",
                                                              defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return outwardDispatchAsyncOverAsync(paymentType, legacyPayload, contentType);
    }

    private Mono<ResponseEntity<?>> outwardDispatch(String paymentType, Object legacyPayload, String inboundContentType) {
        LOG.debug("Legacy outward status request for paymentType '{}' ({})", paymentType, inboundContentType);
        OutStatusRequestProcessingHandler  handler;
        try {
            handler = outStatusHandlerRegistry.getHandler(paymentType);
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return dispatch(paymentType, handler, legacyPayload, "outward", inboundContentType);
    }
    private Mono<ResponseEntity<?>> outwardDispatchAsyncOverAsync(String paymentType, Object legacyPayload, String inboundContentType) {
        LOG.debug("Legacy outward status request for paymentType '{}' ({})", paymentType, inboundContentType);
        OutStatusRequestProcessingHandler handler;
        try {
            handler = outStatusHandlerRegistry.getHandler(paymentType);
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return dispatchSyncOverAsync(paymentType, handler, legacyPayload, "outward", inboundContentType);
    }

    private String outwardAsyncDispatch(String paymentType, Object legacyPayload) {
        LOG.debug("Legacy outward status request for paymentType '{}'", paymentType);
        OutStatusRequestProcessingHandler handler;
        try {
            handler = outStatusHandlerRegistry.getHandler(paymentType);
        } catch (IllegalArgumentException e) {
            return "BAD_REQUEST";
        }
        return dispatchAsync(paymentType, handler, legacyPayload, "outward");
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

        return reactiveStatusService.statusProcessingRequest(converted, direction)
                .map(result -> renderLegacyResponse(adapter, result, inboundContentType, paymentType));
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
        return reactiveStatusService.statusProcessingRequest(converted, "outward")
                .then(Mono.defer(() -> {
                    LOG.info("Sync over async waiting for result: {}", converted);
                    return paymentRendezvous.park(converted.getDetails().getRequestId());
                })).map(result -> renderLegacyResponse(adapter, result, inboundContentType, paymentType));

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

        return reactiveStatusService.statusProcessingRequestAsync(converted, direction);
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
