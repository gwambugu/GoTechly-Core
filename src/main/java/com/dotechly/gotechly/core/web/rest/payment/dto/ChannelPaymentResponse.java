package com.dotechly.gotechly.core.web.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * The channel-facing payment response.
 *
 * <p>{@code PaymentLogSPI} is the pipeline's internal working object — plugins, CBS participants,
 * {@code db-logger} and the audit snapshotter all write into it freely. Returning it directly made
 * every internal scratch field part of the public API contract, including the signed provider wire
 * trace ({@code psp.request}/{@code psp.response}, complete with our own X.509 signing chain), the
 * serialized CBS ISO message, and internal infrastructure URLs.
 *
 * <p>This record is the contract instead: the fields a channel actually needs to reconcile a
 * transaction and decide what to do next. Everything else stays inside — {@code payment_log} and
 * the audit trail still receive the full object.
 *
 * <p>Serialized with {@code NON_NULL}, so a response carries only the fields that have a value.
 *
 * @param requestId          the caller's own correlation id, echoed back
 * @param paymentType        e.g. {@code RNDPS}, {@code MPESA}
 * @param requestType        the phase this response answers, e.g. {@code OUT_PAYMENT}
 * @param status             canonical outcome — {@code SUCCESS} | {@code PENDING} | {@code FAILED}.
 *                           Stable across payment types; this is what channel logic should branch on
 * @param responseCode       canonical code; currently the provider code, reserved for a gateway-owned
 *                           code set
 * @param responseDesc       human-readable description, sourced from {@code error_codes.error_description}
 * @param providerCode       the raw provider code ({@code PDNG}, {@code ACSC}, …) for ops and support
 * @param amount             transaction amount
 * @param currencyCode       transaction currency
 * @param chargeAmount       charge applied, if any
 * @param chargeCurrencyCode charge currency, if any
 * @param gatewayRef         our reference for this transaction
 * @param providerRef        the provider's reference, once known
 * @param responseTimestamp  when the outcome was determined
 * @param resolved           beneficiary details resolved during validation; absent on other phases
 * @param additionalFields   extension point for fields a future corridor needs to return — see the
 *                           contract note below
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChannelPaymentResponse(
        String requestId,
        String paymentType,
        String requestType,
        String status,
        String responseCode,
        String responseDesc,
        String providerCode,
        BigDecimal amount,
        String currencyCode,
        BigDecimal chargeAmount,
        String chargeCurrencyCode,
        String gatewayRef,
        String providerRef,
        Instant responseTimestamp,
        Resolved resolved,
        String partyId,
        Map<String, String> additionalFields
) {

    /** Canonical status values. Provider vocabulary is carried separately in {@code providerCode}. */
    public static final String SUCCESS = "SUCCESS";
    public static final String PENDING = "PENDING";
    public static final String FAILED = "FAILED";

    /*
     * Contract note on additionalFields
     * ---------------------------------
     * This map exists so a corridor can return something the fixed fields don't cover without
     * reshaping the record for every channel. It is NOT a passthrough of
     * Details.additionalFields — that map is internal scratch space (ISO msgIds, settlement dates,
     * instructing-agent codes, the cbc.iso.request.* CBS replay tokens) and bulk-copying it here
     * would reintroduce exactly the leak this DTO removes.
     *
     * The rule: ChannelResponseMapper never auto-populates this map. Every key is added
     * deliberately, by name, by code that has decided that key is safe and useful for a channel to
     * see. Prefer promoting a recurring key to a real field on this record; reach for the map when
     * the key is genuinely corridor-specific.
     */

    /**
     * Beneficiary details the provider resolved during the validation phase. This is genuinely new
     * information — on an R-NDPS account lookup the scheme is authoritative for the account holder's
     * name — so it is the one place the response echoes party data back.
     *
     * @param accountName   the name the provider holds for the account
     * @param accountNumber masked to the last four digits
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Resolved(String accountName, String accountNumber) {}
}
