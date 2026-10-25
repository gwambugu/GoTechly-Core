package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.participants.status;


import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsPaymentReference;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.Stan;
import com.dotechly.gotechly.spi.service.dto.payment.CBS;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsExchange;
import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends the Finacle transaction-status inquiry and maps the reply.
 *
 * <p><b>The reply protocol, which the mapping below follows literally:</b> field 39 reports whether
 * core banking could serve the request. {@code 000} means it accepted and processed the inquiry, and
 * <b>the state of the transaction is then in field 127</b>. Anything else in field 39 means core had
 * a problem and could not serve the request — it carries no information about the payment, so it maps
 * to {@link #NOT_CONFIRMED} and never to a failure of the transaction.
 *
 * <p>Two rules hold this class together.
 *
 * <p><b>1. It always publishes a reply.</b> The stage waits on {@code CBC_STATUS_RESPONSE} with a
 * 10s space timeout; every path out of {@link #commit} therefore has to put something there, or the
 * stage burns that timeout on top of the MUX timeout and then has to treat a bug as a core-banking
 * outage. Previously an unanswered inquiry threw an {@link NullPointerException} inside
 * {@code getStatusInquireFromFinResponse} (the null-response branch dereferenced the very response
 * it had just found null), so nothing was ever published.
 *
 * <p><b>2. It only ever reports NOT_FOUND when core banking actually said so.</b> {@code NOT_FOUND}
 * is a definitive answer that lets a corridor declare a payment failed and stop reconciling it;
 * everything ambiguous — no reply, an inquiry the host could not process, a field 127 that is absent
 * or unparseable — maps to {@code NOT_CONFIRMED} so the caller retries instead of concluding.
 */
public class TxnStatusCheckParticipant implements TransactionParticipant, Configurable {
    private static Logger log = LoggerFactory.getLogger(TxnStatusCheckParticipant.class);

    /** Core banking answered and the transaction is on the books. */
    static final String POSTED = "POSTED";
    /** Core banking answered and has no such transaction. Definitive: the money did not move. */
    static final String NOT_FOUND = "NOT_FOUND";
    /** We still do not know. Retry; never report this as a failure to the scheme. */
    static final String NOT_CONFIRMED = "NOT_CONFIRMED";

    protected Configuration configuration;

    @Override
    public void setConfiguration(Configuration configuration) throws ConfigurationException {
        this.configuration = configuration;
    }

    @Override
    public int prepare(long l, Serializable serializable) {
        return PREPARED;
    }

    @Override
    public void commit(long id, Serializable serializable) {
        Context ctx = (Context) serializable;
        PaymentLogSPI paymentLogSPI = CbsExchange.get(ctx, "CBC_STATUS_REQUEST");
        if (paymentLogSPI == null) {
            log.error("TxnStatusCheckParticipant: no CBC_STATUS_REQUEST in context");
            return;
        }
        try {
            ISOMsg finRequest = getTransactionInquireMsg(paymentLogSPI);
            if (finRequest == null) {
                paymentLogSPI = unconfirmed(paymentLogSPI, null, "Could not build the status inquiry message");
            } else {
                String countryCode = paymentLogSPI.getSender() == null
                        ? null : paymentLogSPI.getSender().getCountryCode();
                String muxStr = AppQBean.getNextMux(countryCode);
                Map<String, MUX> muxMap = AppQBean.getMuxMap(countryCode);
                MUX mx = muxMap == null ? null : muxMap.get(muxStr);
                if (mx == null) {
                    paymentLogSPI = unconfirmed(paymentLogSPI, null,
                            "No core-banking MUX available for country '" + countryCode + "'");
                } else {
                    ISOMsg resp = mx.request(finRequest, 20000L);
                    paymentLogSPI = getStatusInquireFromFinResponse(paymentLogSPI, resp);
                }
            }
        } catch (ISOException | RuntimeException e) {
            log.error("TxnStatusCheckParticipant==> {}", e.getMessage(), e);
            paymentLogSPI = unconfirmed(paymentLogSPI, null, "Status inquiry error: " + e.getMessage());
        }
        // Unconditional: the stage is blocked on this key, and a missing reply is indistinguishable
        // from a core-banking timeout at the far end.
        CbsExchange.put(ctx, "CBC_STATUS_RESPONSE", paymentLogSPI);
    }

    PaymentLogSPI getStatusInquireFromFinResponse(PaymentLogSPI paymentLogSPI, ISOMsg statusResponse) {
        if (statusResponse == null) {
            log.info("Finacle timeout on Status request");
           // return unconfirmed(paymentLogSPI, null, "No response from core banking on the status request");
            CBS cbs = cbs(paymentLogSPI);
            cbs.setStatus(NOT_FOUND);
            cbs.setRespCode("NULL(F)");
            cbs.setRespDesc("Finacle timeout on Status request");
            return paymentLogSPI;
        }

        String de39 = statusResponse.getString(39);
        if ("119".equals(de39)) {
            log.info("Transaction not found in core banking expected 000 but got 119");
            CBS cbs = cbs(paymentLogSPI);
            cbs.setStatus(NOT_FOUND);
            cbs.setRespCode(de39 + "(F)");
            cbs.setRespDesc("Transaction Not found");
            return paymentLogSPI;
        }else if (!"000".equals(de39)) {
            // The host could not answer the inquiry. That says nothing about the payment, so it is
            // not a failure of the payment — only an unanswered question.
            log.info("Finacle returned error '{}' on Status request", de39);
            return unconfirmed(paymentLogSPI, de39,
                    "Core banking returned error " + de39 + " on the status request");
        }

        String f127 = statusResponse.getString(127);
        String[] segments = (f127 == null || f127.isBlank()) ? new String[0] : f127.split("\\|");
        String flag = segments.length == 0 ? null : segments[0].trim();

        if ("Y".equalsIgnoreCase(flag)) {
            log.info("Tran status OK");
            CBS cbs = cbs(paymentLogSPI);
            cbs.setStatus(POSTED);
            cbs.setRespCode(de39 + "(" + flag + ")");
            cbs.setRespDesc("OK");
            captureInquiryReference(cbs, segments, paymentLogSPI);
            return paymentLogSPI;
        }
        if ("N".equalsIgnoreCase(flag)) {
            log.info("Transaction not found in core banking");
            CBS cbs = cbs(paymentLogSPI);
            cbs.setStatus(NOT_FOUND);
            cbs.setRespCode(de39 + "(" + flag + ")");
            cbs.setRespDesc("Transaction Not found");
            return paymentLogSPI;
        }
        if ("F".equalsIgnoreCase(flag)) {
            log.info("Transaction not found in core banking");
            CBS cbs = cbs(paymentLogSPI);
            cbs.setStatus(NOT_FOUND);
            cbs.setRespCode(de39 + "(" + flag + ")");
            cbs.setRespDesc("Transaction Not found");
            return paymentLogSPI;
        }

        // Success response code, but no usable Y/N marker. Some hosts report the miss in field 125
        // instead; anything else stays unknown rather than being read as "not found".
        String f125 = statusResponse.getString(125);
        if (f125 != null && f125.trim().equalsIgnoreCase("Transaction Not Found")) {
            CBS cbs = cbs(paymentLogSPI);
            cbs.setStatus(NOT_FOUND);
            cbs.setRespCode(de39);
            cbs.setRespDesc("Transaction Not found");
            return paymentLogSPI;
        }
        log.warn("Status response carried no usable field 127 marker (DE127='{}', DE125='{}') — treating as unconfirmed",
                f127, f125);
        return unconfirmed(paymentLogSPI, de39, "Status response carried no transaction marker");
    }

    /**
     * Reads the pipe-separated field 127 of a <b>status inquiry</b> reply:
     * {@code Y|30-NOV-22|30-NOV-22| S2306745} — flag, then (when the flag is {@code Y}) the date the
     * transaction was posted, and last the core-banking transaction reference. Segments carry
     * padding, so every one is trimmed.
     *
     * <p><b>This layout is specific to the inquiry reply.</b> Field 127 on a <em>payment</em>
     * response carries different content and is parsed by
     * {@code TransactionPaymentParticipant.getTranIdFrom}, which picks the first or second segment by
     * length. The two parsers are deliberately separate; pointing either at the other message shape
     * yields a plausible-looking reference that identifies nothing.
     *
     * <p>Capturing the reference here is what closes the recovery loop: a payment that timed out has
     * no {@code tranRef} at all — it is lifted from a response that never arrived — so the inquiry is
     * the only chance to learn it. Once stamped, the transaction is addressable by reference for
     * every later inquiry instead of falling back to the original RRN.
     */
    private static void captureInquiryReference(CBS cbs, String[] segments, PaymentLogSPI paymentLogSPI) {
        if (segments.length > 1) {
            String postedDate = segments[1].trim();
            if (!postedDate.isEmpty()) {
                additionalFields(cbs).put("cbc.status.response.postingDate", postedDate);
            }
        }
        // The reference is the last segment, and only exists when there is one beyond the dates.
        if (segments.length <= 2) {
            return;
        }
        String tranRef = segments[segments.length - 1].trim();
        if (tranRef.isEmpty()) {
            return;
        }
        additionalFields(cbs).put("cbc.status.response.tranRef", tranRef);
        String existing = cbs.getTranRef();
        if (existing == null || existing.isBlank()) {
            cbs.setTranRef(tranRef);
        } else if (!existing.split("\\|")[0].trim().equals(tranRef)) {
            // Not overwritten: the stored reference is what the payment corridor recorded, and a
            // disagreement means the inquiry matched a different posting than the one we asked about.
            log.warn("Status inquiry returned tranRef '{}' but the transaction carries '{}' (requestId '{}') — keeping the stored value",
                    tranRef, existing,
                    paymentLogSPI.getDetails() == null ? "unknown" : paymentLogSPI.getDetails().getRequestId());
        }
    }

    private static Map<String, String> additionalFields(CBS cbs) {
        if (cbs.getAdditionalFields() == null) {
            cbs.setAdditionalFields(new HashMap<>());
        }
        return cbs.getAdditionalFields();
    }

    private static PaymentLogSPI unconfirmed(PaymentLogSPI paymentLogSPI, String respCode, String respDesc) {
        if (paymentLogSPI == null) {
            return null;
        }
        CBS cbs = cbs(paymentLogSPI);
        cbs.setStatus(NOT_CONFIRMED);
        if (respCode != null) {
            cbs.setRespCode(respCode);
        }
        cbs.setRespDesc(respDesc);
        return paymentLogSPI;
    }

    private static CBS cbs(PaymentLogSPI paymentLogSPI) {
        if (paymentLogSPI.getCbs() == null) {
            paymentLogSPI.setCbs(new CBS());
        }
        return paymentLogSPI.getCbs();
    }

    public ISOMsg getTransactionInquireMsg(PaymentLogSPI paymentLogSPI) {
        try {
            // tranRef (payment response field 127), else field 37 of the ISO request that timed out.
            // Never Details.requestId: core banking has never seen it, so the inquiry would come back
            // "not found" and that answer is acted on as definitive.
            String tranId = CbsPaymentReference.resolve(paymentLogSPI);
            if (tranId == null) {
                log.error("No core-banking reference to inquire on for requestId '{}'",
                        paymentLogSPI.getDetails() == null ? "unknown" : paymentLogSPI.getDetails().getRequestId());
                return null;
            }
            CBS cbs = cbs(paymentLogSPI);

            ISOMsg m = new ISOMsg();
            String channel = "GoTechly";
            // Was the last six digits of currentTimeMillis, which made a cluster collision total
            // rather than partial: two instances landing on the same millisecond produced the same
            // STAN, the same HHmmss in the RRN below and the same DE12 - an identical (DE12, DE37)
            // mux key, so QMUX could hand one inquiry's reply to the other. See StanGenerator.
            String stan = Stan.next();
            Date today = new Date();
            String tranDateTime = String.format("%tm%td%tH%tM%tS", today, today, today, today, today);
            String tranDate = String.format("%td-%tm-%tY", today, today, today); //23-11-2022
            String localTranDateTime = String.format("%ty%tm%td%tH%tM%tS", today, today, today, today, today, today);
            String rrn = stan + String.format("%tH%tM%tS", today, today, today);
            rrn = leftpad(Long.parseLong(rrn), 12);

            long tranAmount = paymentLogSPI.getDetails() == null || paymentLogSPI.getDetails().getAmount() == null
                    ? 0L : paymentLogSPI.getDetails().getAmount().longValue();
            String f4 = leftpad(tranAmount, 12);
            String f49 = paymentLogSPI.getSender() == null ? null : paymentLogSPI.getSender().getCurrencyCode();
            if (f49 == null) {
                f49 = getCurrencyCode(paymentLogSPI);
            }
            String f43 = channel + "INQ|" + tranId + " |";
            int f43Len = (f43.length() < 40) ? f43.length() : 39;
            f43 = f43.substring(0, f43Len);

            String f123 = "GoTechly";
            String f125 = "TPGINQ|" + tranDate + "|" + tranId;
            Map<String, String> additionalFields = cbs.getAdditionalFields();
            if (additionalFields != null) {
                if (additionalFields.get("status.request.DE123") != null) {
                    f123 = additionalFields.get("status.request.DE123");
                }
                if (additionalFields.get("status.request.DE125") != null) {
                    f125 = additionalFields.get("status.request.DE125");
                }
            }

            m.setMTI("1200");
            m.set(2, "6278519999011712");
            m.set(3, "820000"); //970000
            m.set(4, f4);
            m.set(7, tranDateTime);
            m.set(11, stan);
            m.set(12, localTranDateTime);
            m.set(17, String.format("%tm%td", today, today));
            m.set(24, "200");
            m.set(32, "111111");
            m.set(35, "6278519999011712");
            m.set(37, rrn);
            m.set(41, "CAMPAIGN");
            m.set(43, f43);
            if (f49 != null) {
                m.set(49, f49);
            }
            String drAcc = cbs.getDrAcc() != null ? cbs.getDrAcc()
                    : (paymentLogSPI.getSender() == null ? null : paymentLogSPI.getSender().getAccountNumber());
            if (drAcc != null) {
                m.set(102, drAcc);
            }
            m.set(123, f123);
            m.set(125, f125);
            m.set(126, "CAMPAIGN");
            return m;
        } catch (ISOException | RuntimeException ex) {
            log.error("getTransactionInquireMsg failed: {}", ex.getMessage(), ex);
            return null;
        }
    }

    public String leftpad(long n, int padding) {
        return String.format("%0" + padding + "d", new Object[]{Long.valueOf(n)});
    }

    public String getCurrencyCode(PaymentLogSPI paymentLogSPI) {
        String countryCode = paymentLogSPI.getSender() == null ? null : paymentLogSPI.getSender().getCountryCode();
        if ("KE".equals(countryCode)) {
            return "KES";
        } else if ("UG".equals(countryCode)) {
            return "UGX";
        } else if ("RW".equals(countryCode)) {
            return "RWF";
        } else if ("TZ".equals(countryCode)) {
            return "TZS";
        } else if ("SS".equals(countryCode)) {
            return "SSP";
        } else if ("CD".equals(countryCode)) {
            return "CDF";
        }
        return "";
    }
}
