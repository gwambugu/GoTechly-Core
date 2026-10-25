package com.dotechly.gotechly.core.web.rest.payment.rndps;


import com.dotechly.gotechly.core.config.admin.SchemeEchoRegistry;
import com.dotechly.gotechly.core.config.inward.InCreditReversalHandlerRegistry;
import com.dotechly.gotechly.core.config.inward.InPaymentProcessingHandlerRegistry;
import com.dotechly.gotechly.core.config.inward.InStatusProcessingHandlerRegistry;
import com.dotechly.gotechly.core.config.inward.InValidationHandlerRegistry;
import com.dotechly.gotechly.core.service.plugin.payment.ReactivePaymentProcessingService;
import com.dotechly.gotechly.core.service.plugin.result.ReactiveResultProcessingService;
import com.dotechly.gotechly.core.service.plugin.status.ReactiveStatusService;
import com.dotechly.gotechly.core.service.plugin.validation.ReactiveValidationService;
import com.dotechly.gotechly.spi.service.LegacyRequestAdapter;
import com.dotechly.gotechly.spi.service.admin.SchemeEchoException;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.LegacyResponse;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.util.common.PayloadSanitizer;
import com.dotechly.gotechly.spi.service.util.common.UnsafePayloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;


/**
 * RNDPS-native ISO 20022 inbound endpoints. R-NDPS POSTs ISO 20022 messages (signed
 * {@code <BusinessMessage>} envelopes) to operation-named URLs; this resource is the RNDPS-specific
 * wire adapter that resolves the RNDPS inward handler, lets it convert the payload to a
 * {@link PaymentLogSPI}, runs the standard inward pipeline, and asks the handler to render the
 * {@code pacs.002} response. Mirrors {@code LegacyPaymentResource}'s inward dispatch and the MKESHO
 * {@code LinkedWalletBankAccountResource} pattern.
 *
 * <p>Paths sit under the routable {@code /payment/api/**} prefix so no Gateway route change is
 * needed; R-NDPS is configured with the full FSP URL, so the operation-named suffixes match its
 * {@code /iso20022/v1/*} convention.
 */
@RestController
@RequestMapping("/api/v2/rndps/inward/callbacks/fsp")
public class RndpsIso20022Resource {
    private static final Logger LOG = LoggerFactory.getLogger(RndpsIso20022Resource.class);

    private static final String DIRECTION = "inward";
    private static final String CREDIT_REVERSAL_API_TYPE = "credit-reversal";
    private final InValidationHandlerRegistry inValidationHandlerRegistry;
    private final InPaymentProcessingHandlerRegistry inPaymentHandlerRegistry;
    private final InStatusProcessingHandlerRegistry inStatusHandlerRegistry;
    private final ReactiveValidationService reactiveValidationService;
    private final ReactivePaymentProcessingService reactivePaymentProcessingService;
    private final ReactiveStatusService reactiveStatusService;
    private final ReactiveResultProcessingService reactiveResultProcessingService;
    private final InCreditReversalHandlerRegistry inCreditReversalHandlerRegistry;
    private final SchemeEchoRegistry schemeEchoRegistry;

    /** Payment type of the RNDPS handler to dispatch to. */
    @Value("${gotechly.rndps.payment-type:RNDPS}")
    private String paymentType;

    public RndpsIso20022Resource(InValidationHandlerRegistry inValidationHandlerRegistry,
                                 InPaymentProcessingHandlerRegistry inPaymentHandlerRegistry,
                                 InStatusProcessingHandlerRegistry inStatusHandlerRegistry,
                                 ReactiveValidationService reactiveValidationService,
                                 ReactivePaymentProcessingService reactivePaymentProcessingService,
                                 ReactiveStatusService reactiveStatusService,
                                 ReactiveResultProcessingService reactiveResultProcessingService,
                                 InCreditReversalHandlerRegistry inCreditReversalHandlerRegistry,
                                 SchemeEchoRegistry schemeEchoRegistry) {
        this.inValidationHandlerRegistry = inValidationHandlerRegistry;
        this.inPaymentHandlerRegistry = inPaymentHandlerRegistry;
        this.inStatusHandlerRegistry = inStatusHandlerRegistry;
        this.reactiveValidationService = reactiveValidationService;
        this.reactivePaymentProcessingService = reactivePaymentProcessingService;
        this.reactiveStatusService = reactiveStatusService;
        this.reactiveResultProcessingService = reactiveResultProcessingService;
        this.inCreditReversalHandlerRegistry = inCreditReversalHandlerRegistry;
        this.schemeEchoRegistry = schemeEchoRegistry;
    }


    @PostMapping(value = "/credit",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<?>> creditTransfer2(@RequestBody String body,
                                                   @RequestHeader(value = "Content-Type",
                                                           defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        String apiType = "accept-deferred-flow";
        return handle("creditTransfer", body, contentType,
                inPaymentHandlerRegistry::getHandler,
                log -> reactivePaymentProcessingService.paymentProcessingRequest(
                        log, DIRECTION,apiType ), Reply.STATUS_ONLY);
    }


    @PostMapping("/async-credit")
    public String creditAsync(@RequestBody PaymentLogSPI req) {
        LOG.debug("Async flow credit request: {}", req);
        return reactiveValidationService.validateRequestAsync(req, "outward");
    }


    /**
     * camt.003 — account lookup: resolve a beneficiary account/proxy before a transfer. A read-only
     * enquiry, answered with a camt.004 rendered by the plugin.
     *
     * <p>Note that {@code InputsValidationService} (which only the validation phase runs) must
     * tolerate a log with no amount, no currency and a null {@code Sender} for this to reach the
     * pipeline at all.
     */
    @PostMapping(value = "/account_lookup",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<?>> accountLookup(@RequestBody String body,
                                                 @RequestHeader(value = "Content-Type",
                                                         defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return handle("accountLookup", body, contentType,
                inValidationHandlerRegistry::getHandler,
                log -> reactiveValidationService.validateRequest(log, DIRECTION));
    }

    /** pacs.028 — check payment status request. A read-only enquiry against a stored transaction. */
    @PostMapping(value = "/clearing_status",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<?>> checkStatus(@RequestBody String body,
                                               @RequestHeader(value = "Content-Type",
                                                       defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return handle("checkStatus", body, contentType,
                inStatusHandlerRegistry::getHandler,
                log -> reactiveStatusService.statusProcessingRequest(log, DIRECTION));
    }

    /**
     * pacs.007 — payment reversal: back out a credit we already received and posted. Money movement,
     * but not the credit's: it needs a different core-banking operation and acts on an existing
     * transaction, so it runs the inbound {@code credit-reversal} phase
     * ({@code in-credit-reversal} &rarr; the plugin's {@code InwardReversalResultHandler}) rather
     * than sharing pacs.008's stage list and asking every stage in it whether this message is its
     * business.
     */
    @PostMapping(value = "/payment_reversal",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<?>> paymentReversal(@RequestBody String body,
                                                   @RequestHeader(value = "Content-Type",
                                                           defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return handle("PaymentReversal", body, contentType,
                inCreditReversalHandlerRegistry::getHandler,
                log -> reactivePaymentProcessingService.paymentProcessingRequest(
                        log, DIRECTION, CREDIT_REVERSAL_API_TYPE));
    }

    /**
     * pacs.002 — payment status report: R-NDPS reporting the settlement outcome of a payment we
     * originated. A <em>result callback</em> against an existing outward transaction, so it runs the
     * outward {@code result-processing} pipeline ({@code out-result-handler} &rarr;
     * {@code RndpsResultHandler}), not an inbound phase.
     *
     * <p>Always answers 200: R-NDPS replays non-2xx callbacks, and the handler records an
     * unparseable body as PENDING rather than letting it be redelivered forever.
     */
    @PostMapping(value = "/payment_status_report",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<String>> paymentStatusReport(@RequestBody String body,
                                                            @RequestHeader(value = "Content-Type",
                                                                    defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        LOG.debug("RNDPS pacs.002 result callback for paymentType '{}' ({})", paymentType, contentType);
        PaymentLogSPI seed = new PaymentLogSPI();
        Details details = new Details();
        details.setPaymentType(paymentType);
        seed.setDetails(details);

        return reactiveResultProcessingService.resultProcessingRequest(seed, body, "outward")
                .map(response -> ResponseEntity.ok()
                        .header("Content-Type", MediaType.APPLICATION_XML_VALUE)
                        .body(response == null ? "" : response))
                .onErrorResume(e -> {
                    LOG.error("RNDPS result callback processing failed: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.ok(""));
                });
    }

    /** Resolves the phase's handler for a payment type; throws {@code IllegalArgumentException}. */
    @FunctionalInterface
    private interface HandlerLookup {
        Object resolve(String paymentType);
    }
    /** What the HTTP response to R-NDPS carries once the pipeline has run. */
    private enum Reply {
        /** Render the outcome with the plugin's codec — a camt.004 or a pacs.002. */
        ISO_MESSAGE,
        /**
         * 200 and nothing else, for an operation whose answer travels on a later callback. R-NDPS
         * reads only the status code on that path.
         */
        STATUS_ONLY
    }
    /**
     * Convert with the phase handler's {@link LegacyRequestAdapter}, run that phase's pipeline, then
     * render the reply with the same adapter.
     *
     * @param lookup   the phase registry's {@code getHandler}
     * @param pipeline the phase service call
     */
    private Mono<ResponseEntity<?>> handle(String operation, String body, String contentType,
                                           HandlerLookup lookup,
                                           Function<PaymentLogSPI, Mono<PaymentLogSPI>> pipeline) {
        return handle(operation, body, contentType, lookup, pipeline, Reply.ISO_MESSAGE);
    }

    private Mono<ResponseEntity<?>> handle(String operation, String body, String contentType,
                                           HandlerLookup lookup,
                                           Function<PaymentLogSPI, Mono<PaymentLogSPI>> pipeline,
                                           Reply reply) {
        Decoded decoded = decode(operation, body, contentType, lookup);
        if (decoded.failed()) {
            return Mono.just(decoded.failure());
        }

        return pipeline.apply(decoded.log())
                .map(result -> reply == Reply.STATUS_ONLY
                        ? ResponseEntity.ok().build()
                        : renderResponse(decoded.adapter(), result, contentType, operation));
    }

    /**
     * The result of turning an inbound message into a {@link PaymentLogSPI}: either the decoded log
     * plus the codec that produced it, or the response to send instead.
     */
    private record Decoded(LegacyRequestAdapter adapter, PaymentLogSPI log, ResponseEntity<?> failure) {
        static Decoded of(LegacyRequestAdapter adapter, PaymentLogSPI log) {
            return new Decoded(adapter, log, null);
        }

        static Decoded rejected(ResponseEntity<?> failure) {
            return new Decoded(null, null, failure);
        }

        boolean failed() {
            return failure != null;
        }
    }

    /**
     * Resolve the phase's handler and the corridor's wire codec, then decode the payload.
     *
     * @param lookup the phase registry's {@code getHandler} — checked so a phase with no handler
     *               fails as a bad request here rather than deep inside the pipeline
     */
    private Decoded decode(String operation, String body, String contentType, HandlerLookup lookup) {
        LOG.info("RNDPS {} request for paymentType '{}' ({})", operation, paymentType, contentType);
        String screened;
        try {
            screened = PayloadSanitizer.screenXml(body);
        } catch (UnsafePayloadException e) {
            LOG.warn("RNDPS {} payload refused at the boundary [{}]: {}",
                    operation, e.getReasonCode(), e.getMessage());
            return Decoded.rejected(ResponseEntity.badRequest().build());
        }
        Object handler;
        try {
            handler = lookup.resolve(paymentType);
        } catch (IllegalArgumentException e) {
            LOG.warn("No RNDPS handler registered for paymentType '{}' on operation {}",
                    paymentType, operation);
            return Decoded.rejected(ResponseEntity.badRequest().build());
        }

        LegacyRequestAdapter adapter = resolveCodec(handler);
        if (adapter == null) {
            LOG.warn("No RNDPS wire codec (LegacyRequestAdapter) registered for '{}'", paymentType);
            return Decoded.rejected(ResponseEntity.status(501).build());
        }
        LOG.info("RNDPS {} start request \n {} end request",DIRECTION,screened);
        PaymentLogSPI converted;
        try {
            converted = adapter.legacyToPaymentLogSPI(screened);
        } catch (IllegalArgumentException e) {
            LOG.warn("RNDPS {} payload rejected: {}", operation, e.getMessage());
            return Decoded.rejected(ResponseEntity.badRequest().build());
        }
        if (converted == null || converted.getDetails() == null
                || converted.getDetails().getPaymentType() == null) {
            LOG.warn("RNDPS adapter returned an invalid PaymentLogSPI for {}", operation);
            return Decoded.rejected(ResponseEntity.status(500).build());
        }
        if ( converted.getDetails().getRequestId() != null
                && converted.getDetails().getRequestId().contains("/*</script>")) {
            LOG.warn("RNDPS adapter returned an invalid PaymentLogSPI for {}", operation);
            return Decoded.rejected(ResponseEntity.status(500).build());
        }
        if ( converted.getDetails().getPaymentType() != null
                && converted.getDetails().getPaymentType().contains("/*</script>")) {
            LOG.warn("RNDPS adapter returned an invalid PaymentLogSPI for {}", operation);
            return Decoded.rejected(ResponseEntity.status(500).build());
        }
        return Decoded.of(adapter, converted);
    }

    /**
     * The corridor's wire codec: the plugin class that verifies the {@code <BusinessMessage>}
     * signature, decodes the payload into a {@link PaymentLogSPI} and renders the reply.
     *
     * <p>Usually it <em>is</em> the phase's handler — {@code RndpsInboundAdapter} implements the
     * three inbound handler interfaces alongside {@link LegacyRequestAdapter}, because one codec
     * serves messages belonging to different phases. The exception is {@code credit-reversal}, whose
     * handler ({@code InwardReversalResultHandler}) owns reversal logic and not the wire format, so
     * the codec is looked up from the phases that do carry it. Decoding is a corridor-level concern;
     * only the phase handler's <em>logic</em> is per-phase.
     */
    private LegacyRequestAdapter resolveCodec(Object phaseHandler) {
        if (phaseHandler instanceof LegacyRequestAdapter adapter) {
            return adapter;
        }
        for (HandlerLookup lookup : List.<HandlerLookup>of(
                inPaymentHandlerRegistry::getHandler,
                inValidationHandlerRegistry::getHandler,
                inStatusHandlerRegistry::getHandler)) {
            try {
                if (lookup.resolve(paymentType) instanceof LegacyRequestAdapter adapter) {
                    return adapter;
                }
            } catch (IllegalArgumentException e) {
                // That phase has no handler registered; try the next one.
            }
        }
        return null;
    }

    private ResponseEntity<?> renderResponse(LegacyRequestAdapter adapter, PaymentLogSPI result,
                                             String contentType, String operation) {
        try {
            LegacyResponse legacy = adapter.toLegacyResponse(result, contentType);
            if (legacy != null && legacy.body() != null) {
                String ct = legacy.contentType() != null ? legacy.contentType() : MediaType.APPLICATION_XML_VALUE;
                return ResponseEntity.status(legacy.httpStatus())
                        .header("Content-Type", ct).body(legacy.body());
            }
        } catch (Exception e) {
            LOG.warn("RNDPS {} response rendering failed; falling back to JSON: {}", operation, e.getMessage(), e);
        }
        return ResponseEntity.ok(result);
    }
    /**
     * admn.005 {@code AdmnEchoReq} &rarr; admn.006 {@code AdmnEchoResp} — R-NDPS's heartbeat.
     *
     * <p><strong>Deliberately not a pipeline call.</strong> An echo carries no debtor, creditor,
     * amount or settlement date, so there is no honest {@code PaymentLogSPI} and no phase it belongs
     * to; Core resolves the plugin from {@link SchemeEchoRegistry} and calls it directly, as it does
     * for account linkage. Inventing an {@code echo} apiType to get a stage list would resolve no
     * stages and fail silently.
     *
     * <p><strong>It answers for the listener, not for the corridor.</strong> The reply says this
     * instance is up and can parse and sign a scheme message. It deliberately does not consult the
     * database or core banking: a heartbeat that fails when a dependency wobbles invites R-NDPS to
     * withdraw us entirely, turning a partial outage into a total one. The consequence to be aware
     * of is the mirror image — we keep answering ACTC while payments fail — which is what the
     * transaction-level monitoring is for.
     *
     * <p>The admn DOM and the signing keys stay in the plugin; this method only moves bytes.
     */
    @PostMapping(value = "/echo",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> echo(@RequestBody String body) {
        LOG.debug("RNDPS admn.005 echo request for paymentType '{}'", paymentType);

        var handler = schemeEchoRegistry.findByPaymentType(paymentType);
        if (handler.isEmpty()) {
            LOG.error("RNDPS echo received but no scheme-echo handler is registered for paymentType '{}' "
                    + "— the heartbeat will fail until the plugin is loaded", paymentType);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No echo handler registered for paymentType " + paymentType);
        }

        try {
            String reply = handler.get().echo(body);
            return ResponseEntity.ok()
                    .header("Content-Type", MediaType.APPLICATION_XML_VALUE)
                    .body(reply);
        } catch (SchemeEchoException e) {
            // The request itself was unusable — malformed, or unsigned where a signature is
            // required. That is the caller's problem to fix, so say so rather than answering ACTC.
            LOG.warn("RNDPS echo rejected for paymentType '{}': {}", paymentType, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            LOG.error("RNDPS echo handler failed for paymentType '{}': {}", paymentType, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Echo handler error");
        }
    }
}
