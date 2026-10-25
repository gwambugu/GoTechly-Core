package com.dotechly.gotechly.core.web.rest.payment.pesalink;


import com.dotechly.gotechly.core.config.inward.InValidationHandlerRegistry;
import com.dotechly.gotechly.core.service.plugin.validation.ReactiveValidationService;
import com.dotechly.gotechly.spi.service.LegacyRequestAdapter;
import com.dotechly.gotechly.spi.service.dto.payment.LegacyResponse;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.inward.InValidationRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Pesalink-native ISO 20022 inbound endpoints. IPS forwards ISO 20022 messages (bare
 * {@code <Document>} carrying an enveloped W3C signature) to operation-named URLs; this resource is
 * the Pesalink-specific wire adapter that resolves the Pesalink inward handler, lets it verify and
 * convert the payload to a {@link PaymentLogSPI}, runs the standard inward pipeline, and asks the
 * handler to render the {@code pacs.002} response. Mirrors {@code RndpsIso20022Resource} and
 * {@code LegacyPaymentResource}'s inward dispatch.
 *
 * <p>Paths sit under the routable {@code /payment/api/**} prefix so no Gateway route change is
 * needed. All operations share one dispatch — the adapter detects the concrete message type from the
 * document namespace, so operation-named URLs are for wire fidelity + logging.
 */
@RestController
@RequestMapping("/payment/api/v1/pesalink")
public class PesalinkIso20022Resource {

    private static final Logger LOG = LoggerFactory.getLogger(PesalinkIso20022Resource.class);

    private final InValidationHandlerRegistry inValidationHandlerRegistry;
    private final ReactiveValidationService reactiveValidationService;

    /** Payment type of the Pesalink handler to dispatch to. */
    @Value("${gotechly.pesalink.payment-type:PESALINK}")
    private String paymentType;

    public PesalinkIso20022Resource(InValidationHandlerRegistry inValidationHandlerRegistry,
                                    ReactiveValidationService reactiveValidationService) {
        this.inValidationHandlerRegistry = inValidationHandlerRegistry;
        this.reactiveValidationService = reactiveValidationService;
    }

    /** pacs.008 — credit transfer forwarded by IPS to the creditor agent (GoTechly). */
    @PostMapping(value = "/creditTransfer",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<?>> creditTransfer(@RequestBody String body,
                                                  @RequestHeader(value = "Content-Type",
                                                          defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return handle("creditTransfer", body, contentType);
    }

    /** pacs.002 — payment status report. */
    @PostMapping(value = "/paymentStatusReport",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<?>> paymentStatusReport(@RequestBody String body,
                                                       @RequestHeader(value = "Content-Type",
                                                               defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return handle("paymentStatusReport", body, contentType);
    }

    /** pacs.028 — status request. */
    @PostMapping(value = "/statusRequest",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<?>> statusRequest(@RequestBody String body,
                                                 @RequestHeader(value = "Content-Type",
                                                         defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return handle("statusRequest", body, contentType);
    }

    /** acmt.023 — identification/account verification request. */
    @PostMapping(value = "/accountVerification",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE})
    public Mono<ResponseEntity<?>> accountVerification(@RequestBody String body,
                                                       @RequestHeader(value = "Content-Type",
                                                               defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return handle("accountVerification", body, contentType);
    }

    private Mono<ResponseEntity<?>> handle(String operation, String body, String contentType) {
        LOG.debug("Pesalink {} request for paymentType '{}' ({})", operation, paymentType, contentType);
        InValidationRequestHandler handler;
        try {
            handler = inValidationHandlerRegistry.getHandler(paymentType);
        } catch (IllegalArgumentException e) {
            LOG.warn("No Pesalink inward handler registered for paymentType '{}'", paymentType);
            return Mono.just(ResponseEntity.badRequest().build());
        }
        if (!(handler instanceof LegacyRequestAdapter adapter)) {
            LOG.warn("Pesalink handler for '{}' does not implement LegacyRequestAdapter", paymentType);
            return Mono.just(ResponseEntity.status(501).build());
        }

        PaymentLogSPI converted;
        try {
            converted = adapter.legacyToPaymentLogSPI(body);
        } catch (IllegalArgumentException e) {
            LOG.warn("Pesalink {} payload rejected: {}", operation, e.getMessage());
            return Mono.just(ResponseEntity.badRequest().build());
        }
        if (converted == null || converted.getDetails() == null
                || converted.getDetails().getPaymentType() == null) {
            LOG.warn("Pesalink adapter returned an invalid PaymentLogSPI for {}", operation);
            return Mono.just(ResponseEntity.status(500).build());
        }

        return reactiveValidationService.validateRequest(converted, "inward")
                .map(result -> renderResponse(adapter, result, contentType, operation));
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
            LOG.warn("Pesalink {} response rendering failed; falling back to JSON: {}", operation, e.getMessage(), e);
        }
        return ResponseEntity.ok(result);
    }
}
