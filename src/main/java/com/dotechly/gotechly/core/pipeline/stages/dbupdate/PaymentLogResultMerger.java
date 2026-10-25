package com.dotechly.gotechly.core.pipeline.stages.dbupdate;

import com.dotechly.gotechly.spi.service.dto.payment.CBS;
import com.dotechly.gotechly.spi.service.dto.payment.Details;
import com.dotechly.gotechly.spi.service.dto.payment.PSP;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies a provider result (pacs.002, MPESA result callback, …) onto the original payment record.
 *
 * <p>A result callback arrives as a <em>seed</em> {@link PaymentLogSPI}: the protocol resource
 * builds an empty log, the handler parses the provider payload into it, and only the outcome fields
 * are populated. Persisting that object directly writes a second row — it has no {@code id}, so JPA
 * treats it as an insert, and the originating transaction is never updated.
 *
 * <p>The merge is deliberately one-directional and narrow: <strong>a result may report an outcome,
 * it may not restate the transaction.</strong> Origination fields (identity, parties, and the
 * financial terms) are always taken from the stored record, so a malformed or hostile callback
 * cannot rewrite the amount, currency or beneficiary of a payment that has already been sent.
 * Everything the result legitimately carries — status, response codes, the PSP/CBS exchange — wins
 * when present and leaves the stored value intact when absent.
 */
@Component
public class PaymentLogResultMerger {

    /**
     * Returns the stored record with the result applied. The {@code original} instance is mutated
     * and returned, so it keeps its database identity.
     *
     * @param original the record loaded from {@code payment_log}
     * @param result   the seed log the result handler populated
     */
    public PaymentLogSPI merge(PaymentLogSPI original, PaymentLogSPI result) {
        if (result == null) {
            return original;
        }
        mergeDetails(original.getDetails(), result.getDetails());
        original.setPsp(mergePsp(original.getPsp(), result.getPsp()));
        original.setCbs(mergeCbs(original.getCbs(), result.getCbs()));

        // Parties are origination data; a result callback does not carry them. Only adopt the
        // result's copy if the stored record somehow has none.
        if (original.getSender() == null) {
            original.setSender(result.getSender());
        }
        if (original.getReceiver() == null) {
            original.setReceiver(result.getReceiver());
        }
        return original;
    }

    /**
     * Outcome fields move; identity, origination and financial terms do not.
     *
     * <p>Notably {@code amount}, {@code currencyCode} and the charge pair are never taken from the
     * result — a pacs.002 carries no amount, and accepting one would let a callback silently
     * restate what was paid.
     */
    private void mergeDetails(Details original, Details result) {
        if (original == null || result == null) {
            return;
        }
        original.setStatus(firstNonNull(result.getStatus(), original.getStatus()));
        original.setResponseCode(firstNonNull(result.getResponseCode(), original.getResponseCode()));
        original.setResponseDesc(firstNonNull(result.getResponseDesc(), original.getResponseDesc()));
        original.setResponseTimestamp(firstNonNull(result.getResponseTimestamp(), original.getResponseTimestamp()));
        original.setStatusResponseCode(firstNonNull(result.getStatusResponseCode(), original.getStatusResponseCode()));
        original.setStatusResponseDescription(
                firstNonNull(result.getStatusResponseDescription(), original.getStatusResponseDescription()));
        original.setStatusTimestamp(firstNonNull(result.getStatusTimestamp(), original.getStatusTimestamp()));
        original.setStatusCount(firstNonNull(result.getStatusCount(), original.getStatusCount()));
        original.setProcessingCode(firstNonNull(result.getProcessingCode(), original.getProcessingCode()));
        original.setApiMode(firstNonNull(result.getApiMode(), original.getApiMode()));
        original.setFreeText1(firstNonNull(result.getFreeText1(), original.getFreeText1()));
        original.setFreeText2(firstNonNull(result.getFreeText2(), original.getFreeText2()));
        original.setAdditionalFields(mergeMaps(original.getAdditionalFields(), result.getAdditionalFields()));
        original.setUpdatedAt(Timestamp.from(Instant.now()));
    }

    /**
     * The stored PSP block holds the request leg (what we sent, when, to which host); the result
     * holds the response leg. Field-level non-null-wins keeps both halves of the exchange.
     */
    private PSP mergePsp(PSP original, PSP result) {
        if (result == null) {
            return original;
        }
        if (original == null) {
            return result;
        }
        original.setRequestID(firstNonNull(result.getRequestID(), original.getRequestID()));
        original.setPspId(firstNonNull(result.getPspId(), original.getPspId()));
        original.setRequestTime(firstNonNull(result.getRequestTime(), original.getRequestTime()));
        original.setNarration(firstNonNull(result.getNarration(), original.getNarration()));
        original.setHostURL(firstNonNull(result.getHostURL(), original.getHostURL()));
        original.setDrAccount(firstNonNull(result.getDrAccount(), original.getDrAccount()));
        original.setCrAccount(firstNonNull(result.getCrAccount(), original.getCrAccount()));
        original.setStatus(firstNonNull(result.getStatus(), original.getStatus()));
        original.setApiMode(firstNonNull(result.getApiMode(), original.getApiMode()));
        original.setRequest(firstNonNull(result.getRequest(), original.getRequest()));
        original.setResponse(firstNonNull(result.getResponse(), original.getResponse()));
        original.setResponseTime(firstNonNull(result.getResponseTime(), original.getResponseTime()));
        original.setResponseCode(firstNonNull(result.getResponseCode(), original.getResponseCode()));
        original.setResponseDesc(firstNonNull(result.getResponseDesc(), original.getResponseDesc()));
        original.setResult(firstNonNull(result.getResult(), original.getResult()));
        original.setResultCode(firstNonNull(result.getResultCode(), original.getResultCode()));
        original.setResultDesc(firstNonNull(result.getResultDesc(), original.getResultDesc()));
        original.setReferenceCode(firstNonNull(result.getReferenceCode(), original.getReferenceCode()));
        original.setResultTime(firstNonNull(result.getResultTime(), original.getResultTime()));
        original.setRetryCount(firstNonNull(result.getRetryCount(), original.getRetryCount()));
        original.setRevRespCode(firstNonNull(result.getRevRespCode(), original.getRevRespCode()));
        original.setRevRespDesc(firstNonNull(result.getRevRespDesc(), original.getRevRespDesc()));
        original.setAdditionalFields(mergeMaps(original.getAdditionalFields(), result.getAdditionalFields()));
        return original;
    }

    private CBS mergeCbs(CBS original, CBS result) {
        if (result == null) {
            return original;
        }
        if (original == null) {
            return result;
        }
        original.setRequestId(firstNonNull(result.getRequestId(), original.getRequestId()));
        original.setRequest(firstNonNull(result.getRequest(), original.getRequest()));
        original.setReqTime(firstNonNull(result.getReqTime(), original.getReqTime()));
        original.setNarration(firstNonNull(result.getNarration(), original.getNarration()));
        original.setHost(firstNonNull(result.getHost(), original.getHost()));
        original.setDrAcc(firstNonNull(result.getDrAcc(), original.getDrAcc()));
        original.setCrAcc(firstNonNull(result.getCrAcc(), original.getCrAcc()));
        original.setTransactionCode(firstNonNull(result.getTransactionCode(), original.getTransactionCode()));
        original.setStatus(firstNonNull(result.getStatus(), original.getStatus()));
        original.setApiMode(firstNonNull(result.getApiMode(), original.getApiMode()));
        original.setRespCode(firstNonNull(result.getRespCode(), original.getRespCode()));
        original.setRespDesc(firstNonNull(result.getRespDesc(), original.getRespDesc()));
        original.setTranRef(firstNonNull(result.getTranRef(), original.getTranRef()));
        original.setRespTime(firstNonNull(result.getRespTime(), original.getRespTime()));
        original.setRetryCount(firstNonNull(result.getRetryCount(), original.getRetryCount()));
        original.setRevRespCode(firstNonNull(result.getRevRespCode(), original.getRevRespCode()));
        original.setRevRespDesc(firstNonNull(result.getRevRespDesc(), original.getRevRespDesc()));
        original.setAdditionalFields(mergeMaps(original.getAdditionalFields(), result.getAdditionalFields()));
        return original;
    }

    /** Union of both maps; keys present in the result win. */
    private Map<String, String> mergeMaps(Map<String, String> original, Map<String, String> result) {
        if (result == null || result.isEmpty()) {
            return original;
        }
        Map<String, String> merged = new LinkedHashMap<>();
        if (original != null) {
            merged.putAll(original);
        }
        merged.putAll(result);
        return merged;
    }

    private <T> T firstNonNull(T preferred, T fallback) {
        return preferred != null ? preferred : fallback;
    }
}
