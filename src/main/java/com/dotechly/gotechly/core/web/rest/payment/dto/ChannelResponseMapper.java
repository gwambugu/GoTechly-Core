package com.dotechly.gotechly.core.web.rest.payment.dto;


import com.dotechly.gotechly.core.service.impl.ErrorCodesService;
import com.dotechly.gotechly.spi.service.dto.payment.*;
import com.dotechly.gotechly.spi.service.enumeration.ResponseCodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps the pipeline's {@link PaymentLogSPI} down to the {@link ChannelPaymentResponse} contract.
 *
 * <p>Applied at the REST/callback boundary only — never as a pipeline stage. {@code db-logger},
 * {@code DBUpdateStage} and {@code AuditPipelineListener} all consume the full object downstream,
 * so narrowing it inside the pipeline would blind the audit trail.
 *
 * <p>The mapping is an allowlist: fields are copied out by name. Anything a plugin or CBS
 * participant adds to the working object is invisible to channels until someone adds it here.
 */
@Component
public class ChannelResponseMapper {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelResponseMapper.class);

    /** Returned when there is no usable {@code Details} to map — a pipeline failure before any handler ran. */
    private static final String NO_DETAILS_CODE = "GW-01";

    private final ErrorCodesService errorCodesService;
    private final ChannelResponseProperties properties;

    public ChannelResponseMapper(ErrorCodesService errorCodesService, ChannelResponseProperties properties) {
        this.errorCodesService = errorCodesService;
        this.properties = properties;
    }

    /**
     * Derives the {@code error_codes.api_name} service scope from the pipeline coordinates.
     *
     * <p>Taken from the phase rather than the caller-supplied {@code Details.requestType}: the
     * caller controls that field, and the scope decides which error-code table we resolve status
     * and description from.
     */
    public static String scopeFor(String direction, String pipelinePhase) {
        String prefix = "inward".equalsIgnoreCase(direction) ? "IN" : "OUT";
        String phase = (pipelinePhase == null) ? "" : pipelinePhase;
        String suffix = switch (phase) {
            case "validation", "async-validation" -> "VALIDATION";
            case "payment-processing" -> "PAYMENT";
            case "status-processing" -> "STATUS";
            case "result-processing", "response-processing" -> "RESULT";
            // Unknown phase: keep it distinguishable rather than silently resolving against the
            // wrong error-code set.
            default -> phase.replace('-', '_').toUpperCase(Locale.ROOT);
        };
        return prefix + "_" + suffix;
    }

    public ChannelPaymentResponse toResponse(PaymentLogSPI log, String serviceScope) {
        if (log == null || log.getDetails() == null) {
            LOG.warn("[CHANNEL-RESPONSE] No Details on the pipeline result for scope '{}'", serviceScope);
            return new ChannelPaymentResponse(
                    null, null, null,
                    ChannelPaymentResponse.FAILED, NO_DETAILS_CODE, "Transaction could not be processed.",
                    null, null, null, null, null, null, null,
                    Instant.now(), null, null,null);
        }
         canonicalStatusForValidation(log);
        Details details = log.getDetails();
        Receiver receiver = log.getReceiver();
        String paymentType = details.getPaymentType();
        String providerCode = details.getResponseCode();
        String apiName = buildApiName(paymentType, serviceScope);
        ResponseCodeStatus state = resolveStatus(details, apiName, providerCode);

        return new ChannelPaymentResponse(
                details.getRequestId(),
                paymentType,
                details.getRequestType(),
                canonicalStatus(state),
                providerCode,
                resolveDescription(apiName, providerCode, details, state),
                providerCode,
                details.getAmount(),
                details.getCurrencyCode(),
                details.getChargeAmount(),
                details.getChargeCurrencyCode(),
                gatewayRef(log),
                providerRef(log),
                responseTimestamp(details, log.getPsp()),
                resolved(log, serviceScope),
                resolvePartyID(receiver),
                allowlistedAdditionalFields(paymentType, details));
    }

    private String buildApiName(String paymentType, String serviceScope) {
        if (paymentType == null || paymentType.isBlank() || serviceScope == null || serviceScope.isBlank()) {
            return null;
        }
        return paymentType + "." + serviceScope;
    }
    private String resolvePartyID(Receiver receiver){
        if(receiver==null){
            return null;
        }
        return receiver.getPartyId();

    }

    /**
     * Resolves the canonical outcome.
     *
     * <p>A status the pipeline already set in canonical terms wins — a halt sets {@code FAILED}
     * explicitly and there will be no {@code error_codes} row for a gateway halt code. Otherwise the
     * provider code is resolved through the {@code error_codes} table, which is the single source of
     * truth for provider vocabulary. Unknown codes fall to {@code PENDING} per the project-wide rule:
     * a code ops hasn't mapped yet must leave the transaction in flight, never mark it failed.
     */
    private ResponseCodeStatus resolveStatus(Details details, String apiName, String providerCode) {
        String status = details.getStatus();
        if (status != null) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if ("FAILED".equals(normalized) || "REJECTED".equals(normalized)) {
                return ResponseCodeStatus.FAILED;
            }
            if ("COMPLETED".equals(normalized) || "SUCCESS".equals(normalized)) {
                return ResponseCodeStatus.COMPLETED;
            }
            if ("PENDING".equals(normalized)) {
                return ResponseCodeStatus.PENDING;
            }
        }

        if (apiName == null || providerCode == null || providerCode.isBlank()) {
            return ResponseCodeStatus.PENDING;
        }

        ResponseCodeStatus mapped = errorCodesService.findAllByApiName(apiName).get(providerCode);
        if (mapped == null) {
            LOG.warn("[CHANNEL-RESPONSE] Unmapped provider code '{}' for api_name='{}' — reporting PENDING. "
                    + "Add an error_codes row to classify it.", providerCode, apiName);
            return ResponseCodeStatus.PENDING;
        }
        return mapped;
    }

    private String canonicalStatus(ResponseCodeStatus state) {
        return switch (state) {
            case COMPLETED -> ChannelPaymentResponse.SUCCESS;
            case FAILED -> ChannelPaymentResponse.FAILED;
            case PENDING -> ChannelPaymentResponse.PENDING;
        };
    }
    private PaymentLogSPI canonicalStatusForValidation(PaymentLogSPI log) {
        if(log.getDetails().getRequestType().contains("OUT_VALIDATION") || log.getDetails().getRequestType().contains("VALIDATION_REQUEST")  ){
            if (log.getPsp() != null) {
                String status = log.getPsp().getStatus();
                if ("RESOLVED".equalsIgnoreCase(status)) {
                    log.getDetails().setStatus("COMPLETED");
                }
                if ("REJECTED".equalsIgnoreCase(status) || "RJCT".equalsIgnoreCase(status) ) {
                    log.getDetails().setStatus("FAILED");
                }
            }
        }
        String detailsRespCode = (log.getDetails().getResponseCode()==null)? "" : log.getDetails().getResponseCode();
        if(detailsRespCode.startsWith("VL-")){
            log.getDetails().setStatus("FAILED");
        }
        return log;
    }

    /**
     * Prefers the operator-maintained {@code error_codes.error_description} — that column exists so
     * a channel gets a sentence rather than a restated code. Falls back to whatever the plugin put
     * on the log, then to a generic line keyed on the canonical status.
     */
    private String resolveDescription(String apiName, String providerCode, Details details, ResponseCodeStatus state) {
        if (apiName != null && providerCode != null && !providerCode.isBlank()) {
            String described = errorCodesService.findDescriptionsByApiName(apiName).get(providerCode);
            if (described != null && !described.isBlank()) {
                return described;
            }
        }
        String pluginDesc = details.getResponseDesc();
        if (pluginDesc != null && !pluginDesc.isBlank()) {
            return pluginDesc;
        }
        return switch (state) {
            case COMPLETED -> "Transaction completed successfully.";
            case FAILED -> "Transaction failed.";
            case PENDING -> "Transaction accepted and is being processed.";
        };
    }

    /** Our own reference for the transaction. */
    private String gatewayRef(PaymentLogSPI log) {
        CBS cbs = log.getCbs();
        if (cbs != null && isPresent(cbs.getTranRef())) {
            return cbs.getTranRef();
        }
        Details details = log.getDetails();
        if (details != null && details.getAdditionalFields() != null) {
            String msgId = details.getAdditionalFields().get("msgId");
            if (isPresent(msgId)) {
                return msgId;
            }
        }
        return null;
    }

    /** The provider's reference, once it has issued one. */
    private String providerRef(PaymentLogSPI log) {
        PSP psp = log.getPsp();
        if (psp == null) {
            return null;
        }
        if (isPresent(psp.getReferenceCode())) {
            return psp.getReferenceCode();
        }
        return isPresent(psp.getPspId()) ? psp.getPspId() : null;
    }

    private Instant responseTimestamp(Details details, PSP psp) {
        Timestamp detailsTs = details.getResponseTimestamp();
        if (detailsTs != null) {
            return detailsTs.toInstant();
        }
        if (psp != null && psp.getResponseTime() != null) {
            return psp.getResponseTime().toInstant();
        }
        return null;
    }

    /**
     * Beneficiary details, on the validation phase only. On an account lookup the provider is
     * authoritative for the account holder's name, so this is new information rather than an echo —
     * which is why it is the one place party data comes back.
     */
    private ChannelPaymentResponse.Resolved resolved(PaymentLogSPI log, String serviceScope) {
        if (serviceScope == null || !serviceScope.endsWith("_VALIDATION")) {
            return null;
        }
        Receiver receiver = log.getReceiver();
        if (receiver == null || !isPresent(receiver.getReceiverName())) {
            return null;
        }
        return new ChannelPaymentResponse.Resolved(
                receiver.getReceiverName(),
                receiver.getAccountNumber());//maskAccount(receiver.getAccountNumber()));
    }

    private String maskAccount(String accountNumber) {
        if (!isPresent(accountNumber)) {
            return null;
        }
        String trimmed = accountNumber.trim();
        if (trimmed.length() <= 4) {
            return "****";
        }
        return "****" + trimmed.substring(trimmed.length() - 4);
    }

    /**
     * Copies only the keys named in {@code gotechly.channel-response.additional-fields.{paymentType}}.
     *
     * <p>Deliberately not a bulk copy of {@code Details.additionalFields} — that map is internal
     * scratch space (ISO msgIds, settlement dates, instructing-agent codes, and the
     * {@code cbc.iso.request.*} CBS replay tokens), and passing it through wholesale would undo the
     * point of this DTO.
     */
    private Map<String, String> allowlistedAdditionalFields(String paymentType, Details details) {
        List<String> allowed = properties.allowedKeysFor(paymentType);
        if (allowed.isEmpty() || details.getAdditionalFields() == null) {
            return null;
        }
        Map<String, String> source = details.getAdditionalFields();
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : allowed) {
            String value = source.get(key);
            if (isPresent(value)) {
                out.put(key, value);
            }
        }
        return out.isEmpty() ? null : out;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
