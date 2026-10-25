package com.dotechly.gotechly.core.web.rest.payment.linkedwallet;


import com.dotechly.gotechly.core.config.inward.LinkedWalletBankAccountRegistry;
import com.dotechly.gotechly.core.service.inward.LinkedWalletPipelineService;
import com.dotechly.gotechly.spi.service.dto.payment.LegacyResponse;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.spi.service.inward.linkedwallet.LinkedWalletBankAccountHandler;
import com.dotechly.gotechly.spi.service.inward.linkedwallet.LinkedWalletOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Inbound endpoint for the VMT &harr; FSP linked-wallet SOAP service
 * ({@code I_VMT2FSP_TransactionManagement}). A PSP (M-PESA/VMT) drives a bank savings account
 * (e.g. M-KESHO). This resource is the VMT-specific wire adapter: it maps VMT SOAP operations to
 * the generic {@link LinkedWalletOperation}, resolves the plugin handler, lets the plugin compose a
 * PaymentLogSPI, runs the inbound pipeline (where the CBS money-movement stage slots in), then asks
 * the plugin to render the SOAP response. Mirrors the flow of {@code LegacyPaymentResource}.
 *
 * <p>Another linked-wallet wire protocol would get its own thin resource; the SPI, registry,
 * pipeline service and subscription table are protocol-agnostic.
 */
@RestController
@RequestMapping("/payment/api/v1/legacy/in/VMT.BankingIntegration.FSI")
public class LinkedWalletBankAccountResource {

    private static final Logger LOG = LoggerFactory.getLogger(LinkedWalletBankAccountResource.class);

    private static final String SVC = "/VMT2FSP_TransactionManagement.svc";

    private final LinkedWalletBankAccountRegistry registry;
    private final LinkedWalletPipelineService pipelineService;

    /** Optional explicit paymentType; when blank, the sole registered handler is used. */
    @Value("${gotechly.linked-wallet.default-payment-type:}")
    private String defaultPaymentType;

    public LinkedWalletBankAccountResource(LinkedWalletBankAccountRegistry registry,
                                           LinkedWalletPipelineService pipelineService) {
        this.registry = registry;
        this.pipelineService = pipelineService;
    }

    /** SOAP entry point — the real wire format M-PESA speaks. */
    @PostMapping(value = SVC,
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, "application/soap+xml"})
    public Mono<ResponseEntity<Object>> handleSoap(@RequestBody String soapBody,
                                              @RequestHeader(value = "SOAPAction", required = false) String soapAction,
                                              @RequestHeader(value = "Content-Type",
                                                      defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        LinkedWalletOperation op = resolveOperation(soapAction, null, soapBody);
        if (op == null) {
            LOG.warn("Could not resolve linked-wallet operation (SOAPAction='{}')", soapAction);
            return Mono.just(soapFault(FAULT_CLIENT, "Unrecognised SOAP operation / SOAPAction"));
        }
        return dispatch(op, soapBody, contentType, true);
    }

    /** REST/JSON entry point — operation via {@code ?op=} or {@code X-Linked-Wallet-Operation} header. */
    @PostMapping(value = SVC, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Object>> handleJson(@RequestBody Object jsonBody,
                                              @RequestParam(value = "op", required = false) String opParam,
                                              @RequestHeader(value = "X-Linked-Wallet-Operation", required = false) String opHeader) {
        LinkedWalletOperation op = resolveOperation(null, opParam != null ? opParam : opHeader, null);
        if (op == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return dispatch(op, jsonBody, MediaType.APPLICATION_JSON_VALUE, false);
    }

    /** Health-check / echo — M-PESA availability probe. Resolves to STATUS. */
    @GetMapping(value = SVC)
    public Mono<ResponseEntity<Object>> healthCheck(@RequestHeader(value = "Content-Type",
            defaultValue = MediaType.TEXT_XML_VALUE) String contentType) {
        return dispatch(LinkedWalletOperation.STATUS, "", contentType, true);
    }

    /**
     * @param soap when true the caller spoke SOAP, so error/exception outcomes are returned as a
     *             SOAP 1.1 {@code <soap:Fault>} envelope with HTTP 500 (the fault transport a SOAP
     *             client such as Axis2 expects). Successful transactional outcomes — including
     *             business decline codes carried in {@code <result>} — stay HTTP 200 per the VMT
     *             samples. JSON callers ({@code soap=false}) get plain status codes instead.
     */
    private Mono<ResponseEntity<Object>> dispatch(LinkedWalletOperation op, Object rawRequest,
                                             String inboundContentType, boolean soap) {
        Optional<LinkedWalletBankAccountHandler> resolved = resolveHandler();
        if (resolved.isEmpty()) {
            LOG.warn("No linked-wallet handler registered (defaultPaymentType='{}', registered={})",
                    defaultPaymentType, registry.getRegisteredPaymentTypes());
            return Mono.just(soap
                    ? soapFault(FAULT_SERVER, "No handler available for the requested service")
                    : ResponseEntity.status(404).build());
        }
        LinkedWalletBankAccountHandler handler = resolved.get();

        PaymentLogSPI converted;
        try {
            converted = handler.toPaymentLogSPI(op, rawRequest);
        } catch (IllegalArgumentException e) {
            LOG.warn("Linked-wallet {} payload rejected by handler '{}': {}",
                    op, handler.paymentType(), e.getMessage());
            return Mono.just(soap
                    ? soapFault(FAULT_CLIENT, "Malformed request: " + e.getMessage())
                    : ResponseEntity.badRequest().build());
        }

        if (converted == null || converted.getDetails() == null
                || converted.getDetails().getPaymentType() == null) {
            LOG.warn("Linked-wallet handler '{}' returned an invalid PaymentLogSPI for {}",
                    handler.paymentType(), op);
            return Mono.just(soap
                    ? soapFault(FAULT_SERVER, "Handler produced an invalid request")
                    : ResponseEntity.status(500).build());
        }

        return pipelineService.process(converted, op.apiType())
                .map(result -> renderResponse(handler, op, result, inboundContentType, soap))
                .onErrorResume(e -> {
                    LOG.error("Linked-wallet {} pipeline failed for '{}': {}",
                            op, handler.paymentType(), e.getMessage(), e);
                    ResponseEntity<Object> resp = soap
                            ? soapFault(FAULT_SERVER, "Processing error")
                            : ResponseEntity.status(500).build();
                    return Mono.just(resp);
                });
    }

    /**
     * Ask the handler to render the pipeline outcome. If it returns null (opted out) or throws,
     * fall back to a SOAP fault (SOAP callers) or JSON PaymentLogSPI (REST callers) so a formatting
     * failure never hides the pipeline result or leaks a JSON body to a SOAP client.
     */
    private ResponseEntity<Object> renderResponse(LinkedWalletBankAccountHandler handler, LinkedWalletOperation op,
                                                  PaymentLogSPI result, String inboundContentType, boolean soap) {
        try {
            LegacyResponse rendered = handler.toResponse(op, result, inboundContentType);
            if (rendered != null && rendered.body() != null) {
                String ct = rendered.contentType() != null ? rendered.contentType() : MediaType.TEXT_XML_VALUE;
                // Honor the adapter's HTTP status: a schema-typed <soap:Fault> comes back as 500,
                // success as 200. SOAP 1.1 requires a fault be sent with HTTP 500.
                int status = rendered.httpStatus() > 0 ? rendered.httpStatus() : 200;
                return ResponseEntity.status(status).header("Content-Type", ct).body((Object) rendered.body());
            }
        } catch (Exception e) {
            LOG.warn("Linked-wallet response rendering failed for {} '{}': {}",
                    op, handler.paymentType(), e.getMessage(), e);
            if (soap) {
                return soapFault(FAULT_SERVER, "Response rendering failed");
            }
        }
        return ResponseEntity.<Object>ok(result);
    }

    // =========================================================================
    // SOAP 1.1 fault rendering — a fault is always HTTP 500 with a <soap:Fault> body.
    // =========================================================================

    private static final String SOAP_ENV_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    /** Caller-side fault (bad request). */
    private static final String FAULT_CLIENT = "soap:Client";
    /** Server-side fault (processing error). */
    private static final String FAULT_SERVER = "soap:Server";

    private ResponseEntity<Object> soapFault(String faultCode, String faultString) {
        String xml = "<soap:Envelope xmlns:soap=\"" + SOAP_ENV_NS + "\"><soap:Body><soap:Fault>"
                + "<faultcode>" + faultCode + "</faultcode>"
                + "<faultstring>" + escapeXml(faultString) + "</faultstring>"
                + "</soap:Fault></soap:Body></soap:Envelope>";
        return ResponseEntity.status(500)
                .header("Content-Type", "text/xml; charset=UTF-8")
                .body((Object) xml);
    }

    private static String escapeXml(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private Optional<LinkedWalletBankAccountHandler> resolveHandler() {
        if (defaultPaymentType != null && !defaultPaymentType.isBlank()) {
            return registry.findByPaymentType(defaultPaymentType.trim());
        }
        return registry.findSole();
    }

    /**
     * Resolve the operation from (in priority order) the SOAPAction header, an explicit op token
     * (REST), then a scan of the SOAP body for a known VMT operation element. VMT operation names
     * map to the generic category operations here — this resource is the VMT-specific adapter.
     */
    private LinkedWalletOperation resolveOperation(String soapAction, String opToken, String bodyForScan) {
        // 1. explicit REST token — matches enum name or apiType
        if (opToken != null && !opToken.isBlank()) {
            LinkedWalletOperation byToken = fromToken(opToken.trim());
            if (byToken != null) return byToken;
        }
        // 2. SOAPAction header — take the segment after the last '/', stripped of quotes
        if (soapAction != null && !soapAction.isBlank()) {
            String action = soapAction.trim().replace("\"", "").replace("%22", "");
            int slash = action.lastIndexOf('/');
            String localName = slash >= 0 ? action.substring(slash + 1) : action;
            LinkedWalletOperation byAction = fromVmtOperation(localName);
            if (byAction != null) return byAction;
        }
        // 3. body scan — find the first VMT operation element present
        if (bodyForScan != null && !bodyForScan.isBlank()) {
            for (String vmtOp : VMT_OP_ORDER) {
                if (bodyForScan.contains(vmtOp)) {
                    return fromVmtOperation(vmtOp);
                }
            }
        }
        return null;
    }

    private static final String[] VMT_OP_ORDER = {
            "VMTInitiatedMoneyTransferFromVMTToAccount",
            "VMTInitiatedMoneyTransferFromAccountToVMT",
            "GetAccountBalance",
            "GetAccountTransactionList"
    };

    private static LinkedWalletOperation fromVmtOperation(String localName) {
        if (localName == null) return null;
        return switch (localName) {
            case "VMTInitiatedMoneyTransferFromVMTToAccount" -> LinkedWalletOperation.DEPOSIT;
            case "VMTInitiatedMoneyTransferFromAccountToVMT" -> LinkedWalletOperation.WITHDRAWAL;
            case "GetAccountBalance" -> LinkedWalletOperation.BALANCE;
            case "GetAccountTransactionList" -> LinkedWalletOperation.MINI_STATEMENT;
            default -> null;
        };
    }

    private static LinkedWalletOperation fromToken(String token) {
        for (LinkedWalletOperation op : LinkedWalletOperation.values()) {
            if (op.name().equalsIgnoreCase(token) || op.apiType().equalsIgnoreCase(token)) {
                return op;
            }
        }
        return null;
    }
}
