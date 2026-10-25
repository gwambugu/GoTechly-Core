package com.dotechly.gotechly.core.web.rest.payment.account;


import com.dotechly.gotechly.core.service.account.LinkedAccountRequestException;
import com.dotechly.gotechly.core.service.account.LinkedAccountService;
import com.dotechly.gotechly.core.web.rest.payment.rndps.RndpsIso20022Resource;
import com.dotechly.gotechly.spi.service.outward.account.LinkedAccountOperation;
import com.dotechly.gotechly.spi.service.outward.account.LinkedAccountRequest;
import com.dotechly.gotechly.spi.service.outward.account.LinkedAccountResponse;
import com.dotechly.gotechly.spi.service.outward.account.LinkedAccountStatus;
import com.dotechly.gotechly.spi.service.util.common.PayloadSanitizer;
import com.dotechly.gotechly.spi.service.util.common.UnsafePayloadException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Channel-facing account/alias (proxy) management API, plus the sink for the scheme's asynchronous
 * account-management callbacks.
 *
 * <h3>Routes</h3>
 * <ul>
 *   <li>{@code POST /payment/api/v1/accounts/linkage/register} — link an account and claim an alias</li>
 *   <li>{@code POST /payment/api/v1/accounts/linkage/update} — change the alias on a linked account</li>
 *   <li>{@code POST /payment/api/v1/accounts/linkage/deregister} — delink the account</li>
 *   <li>{@code GET  /payment/api/v1/accounts/linkage/status/{requestId}} — registration status by
 *       correlation id</li>
 *   <li>{@code GET  /payment/api/v1/accounts/linkage/status?paymentType=&accountNumber=|alias=|msisdn=}
 *       — registration status for a channel that kept an identifier rather than the requestId</li>
 *   <li>{@code POST /payment/api/v1/accounts/linkage/callback/{paymentType}} — the scheme's
 *       account-management callback (acmt.010 / acmt.011 / acmt.006 / acmt.014 for R-NDPS)</li>
 * </ul>
 *
 * <h3>Status semantics</h3>
 * The three mutating routes answer {@code 202 Accepted} with {@link LinkedAccountStatus#PENDING}
 * when the scheme has acknowledged but not yet decided — the normal case on a directory scheme —
 * and {@code 200 OK} once a final status is in hand. A scheme decline is an <em>answer</em>, so it
 * comes back {@code 200} carrying {@code REJECTED} rather than as an HTTP error; only Core-side
 * problems (bad request, unknown scheme, no linkage) use 4xx/5xx.
 */
@RestController
@RequestMapping("/payment/api/v1/accounts/linkage")
public class LinkedAccountResource {

    private static final Logger LOG = LoggerFactory.getLogger(LinkedAccountResource.class);

    private final LinkedAccountService service;

    public LinkedAccountResource(LinkedAccountService service) {
        this.service = service;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LinkedAccountResponse>> register(@RequestBody LinkedAccountRequest body) {
        return submit(LinkedAccountOperation.REGISTER, body);
    }

    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LinkedAccountResponse>> update(@RequestBody LinkedAccountRequest body) {
        return submit(LinkedAccountOperation.UPDATE, body);
    }

    @PostMapping(value = "/deregister", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LinkedAccountResponse>> deregister(@RequestBody LinkedAccountRequest body) {
        return submit(LinkedAccountOperation.DEREGISTER, body);
    }

    /**
     * Generic form for channels that prefer one route: {@code POST /{operation}} with
     * {@code operation} in {@code register|update|deregister}. Kept alongside the named routes rather
     * than replacing them so an operation name typo fails at the router instead of resolving to the
     * wrong operation.
     */
    @PostMapping(value = "/operations/{operation}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<LinkedAccountResponse>> operation(@PathVariable("operation") String operation,
                                                                 @RequestBody LinkedAccountRequest body) {
        return submit(LinkedAccountOperation.parse(operation), body);
    }

    private Mono<ResponseEntity<LinkedAccountResponse>> submit(LinkedAccountOperation operation,
                                                               LinkedAccountRequest body) {
        LOG.info("Linked-account {} request for paymentType '{}', account '{}'",
                operation, body == null ? null : body.paymentType(),
                body == null ? null : body.savingsAccountNumber());
        return service.submit(operation, body)
                .map(LinkedAccountResource::statusFor)
                .onErrorResume(LinkedAccountRequestException.class,
                        e -> Mono.just(ResponseEntity.status(map(e.getKind()))
                                .body(errorBody(operation, body, e))));
    }

    /**
     * The scheme's asynchronous verdict. Always 200 once the plugin has parsed it — a directory
     * scheme replays a non-2xx callback, and a row we could not correlate is an integration bug to
     * investigate from the log, not something a redelivery will fix. A parse failure is the one case
     * that answers 400: that tells the counterparty its payload was the problem.
     */
    @PostMapping(value = "/callback/{paymentType}")
    public Mono<ResponseEntity<String>> callback(@PathVariable("paymentType") String paymentType,
                                                 @RequestBody String rawBody) {
        LOG.info("Linked-account callback for paymentType '{}' ({} bytes)",
                paymentType, rawBody == null ? 0 : rawBody.length());
        String screened;
        try {
            screened = PayloadSanitizer.screenXml(rawBody);
        } catch (UnsafePayloadException e) {
            LOG.warn("RNDPS payload refused at the boundary [{}]: {}", e.getReasonCode(), e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body("UNPARSEABLE"));
        }
        LOG.info("***** Linked-account callback start***** \n {} \n **** Linked-account callback end****",screened);
        return service.handleCallback(paymentType, screened)
                .map(parsed -> ResponseEntity.ok("ACCEPTED"))
                .onErrorResume(LinkedAccountRequestException.class, e -> {
                    LOG.warn("Linked-account callback for '{}' rejected: {}", paymentType, e.getMessage());
                    return Mono.just(ResponseEntity.status(map(e.getKind())).body(e.getMessage()));
                })
                .onErrorResume(e -> {
                    LOG.error("Linked-account callback for '{}' could not be parsed", paymentType, e);
                    return Mono.just(ResponseEntity.badRequest().body("UNPARSEABLE"));
                });
    }

    @GetMapping(value = "/status/{requestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LinkedAccountResponse> statusByRequestId(@PathVariable("requestId") String requestId) {
        return service.statusByRequestId(requestId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @GetMapping(value = "/status-by-account/{paymentType}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LinkedAccountResponse> statusByPaymentTypeAndAccountNumber(@PathVariable("paymentType") String paymentType,
                                                                   @RequestParam(value = "accountNumber") String accountNumber) {
        return service.statusByPaymentTypeAndAccountNumber(paymentType,accountNumber)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LinkedAccountResponse> statusByIdentifier(
            @RequestParam("paymentType") String paymentType,
            @RequestParam(value = "accountNumber", required = false) String accountNumber,
            @RequestParam(value = "alias", required = false) String alias,
            @RequestParam(value = "msisdn", required = false) String msisdn) {

        if (isBlank(accountNumber) && isBlank(alias) && isBlank(msisdn)) {
            // Without an identifier this would silently answer about an arbitrary linkage.
            return ResponseEntity.badRequest().build();
        }
        return service.statusByIdentifier(paymentType, accountNumber, alias, msisdn)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---- internals ----------------------------------------------------------

    /** 202 while the scheme is still deciding, 200 once it has answered either way. */
    private static ResponseEntity<LinkedAccountResponse> statusFor(LinkedAccountResponse response) {
        return response.status() == LinkedAccountStatus.PENDING
                ? ResponseEntity.accepted().body(response)
                : ResponseEntity.ok(response);
    }

    private static HttpStatus map(LinkedAccountRequestException.Kind kind) {
        return switch (kind) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case NO_HANDLER -> HttpStatus.NOT_IMPLEMENTED;
        };
    }

    /**
     * A body even on the error paths: the channel gets the requestId and reason in the shape it
     * already parses, rather than an empty response it has to special-case.
     */
    private static LinkedAccountResponse errorBody(LinkedAccountOperation operation,
                                                   LinkedAccountRequest body,
                                                   LinkedAccountRequestException e) {
        return new LinkedAccountResponse(
                body == null ? null : body.requestId(),
                body == null ? null : body.paymentType(),
                operation,
                body == null ? null : body.savingsAccountNumber(),
                body == null ? null : body.alias(),
                LinkedAccountStatus.FAILED,
                null, null,
                e.getKind().name(),
                e.getMessage(),
                null);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
