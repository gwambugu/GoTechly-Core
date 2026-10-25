package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.participants.reversal;


import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsExchange;
import com.dotechly.gotechly.spi.service.util.common.SerializeUtil;
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
import java.util.HashMap;
import java.util.Map;

public class TransactionReversalParticipant implements TransactionParticipant, Configurable {
    private static Logger log = LoggerFactory.getLogger(TransactionReversalParticipant.class);
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
        log.info("Committed Transaction");
        Context ctx = (Context) serializable;

        PaymentLogSPI paymentLogSPI = CbsExchange.get(ctx, "CBC_REVERSAL_REQUEST");
        if (paymentLogSPI == null) {
            log.error("TransactionReversalParticipant: no CBC_REVERSAL_REQUEST in context");
            return;
        }
        try {
            ISOMsg finRequest = createReversalMsg(paymentLogSPI);
            paymentLogSPI = setAdditionalFieldsFromRequest(paymentLogSPI, finRequest);
            String countryCode = paymentLogSPI.getSender() == null
                    ? null : paymentLogSPI.getSender().getCountryCode();
            String muxStr = AppQBean.getNextMux(countryCode);
            Map<String, MUX> muxMap = AppQBean.getMuxMap(countryCode);
            MUX mx = muxMap == null ? null : muxMap.get(muxStr);
            if (finRequest == null || mx == null) {
                log.error("Reversal not sent (finRequest={}, mux={}) for requestId '{}'",
                        finRequest != null, mx != null,
                        paymentLogSPI.getDetails() == null ? "unknown" : paymentLogSPI.getDetails().getRequestId());
            } else {
                ISOMsg resp = mx.request(finRequest, 10000L);
                paymentLogSPI = setAdditionalFieldsFromResponse(paymentLogSPI, resp);
            }
        } catch (ISOException | RuntimeException e) {
            log.error("reversal Participant==> {}", e.getMessage(), e);
        }
        // Unconditional: the stage blocks on this key. A reversal that was not sent leaves CBS.status
        // as it was, so the stage reads it as unconfirmed and the caller retries — which is right,
        // the original posting is still there.
        CbsExchange.put(ctx, "CBC_REVERSAL_RESPONSE", paymentLogSPI);
    }
    public ISOMsg createReversalMsg(PaymentLogSPI paymentLogSPI) {
        try {
            ISOMsg m = (ISOMsg) SerializeUtil.strToObj(new String(paymentLogSPI.getCbs().getRequest()));
            String f56 = m.getMTI() + "000000" + (String) m.getValue(11) + (String) m.getValue(12) + "6" + (String) m.getValue(32);
            m.set(24, "400");
            String charges = m.getString(46);
            if (charges != null) {
                charges = charges.replaceAll("D", "C");
                m.set(46, charges);
            }
            m.set(56, f56);
            m.setMTI("1420");
            return m;
        } catch (ISOException ex) {

            log.error("createReversalMsg ISOException" + ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.error("createReversalMsg Exception" + ex.getMessage());
            return null;
        }
    }
    private PaymentLogSPI setAdditionalFieldsFromRequest(PaymentLogSPI paymentLogSPI,ISOMsg request){
        if(paymentLogSPI.getCbs().getAdditionalFields()==null) {
            paymentLogSPI.getCbs().setAdditionalFields(new HashMap<>());
        }
        if(request!=null){
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.reversal.DE11", request.getString(11));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.reversal.DE37", request.getString(37));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.reversal.DE103", request.getString(103));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.reversal.DE123", request.getString(123));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.reversal.DE125", request.getString(125));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.reversal.DE126", request.getString(126));
        }

        return paymentLogSPI;
    }
    private PaymentLogSPI setAdditionalFieldsFromResponse(PaymentLogSPI paymentLogSPI,ISOMsg response){
        if(response!=null){
            if(paymentLogSPI.getCbs().getAdditionalFields()==null) {
                paymentLogSPI.getCbs().setAdditionalFields(new HashMap<>());
            }
            //paymentLogSPI.getCbs().setRespCode(response.getString(39));
            String tranId = getTranIdFrom(response.getString(127));
            // Deliberately NOT written back to CBS.tranRef. That field holds the *payment's* core
            // banking reference, which is what a later status inquiry keys on; overwriting it with
            // the reversal's own reference means any subsequent reconciliation asks about the
            // reversal instead of the posting it was meant to undo. The reversal reference is kept
            // below under cbc.payment.reversal.response.DE127.
            if("000".equals(response.getString(39))){
                paymentLogSPI.getCbs().setStatus("REVERSED");
            }
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.reversal.response.DE39", response.getString(39));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.reversal.response.DE37", response.getString(37));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.reversal.response.DE127", tranId);
        }

        return paymentLogSPI;
    }
    public String leftpad(long n, int padding) {
        return String.format("%0" + padding + "d", new Object[] { Long.valueOf(n) });
    }

    public String getCurrencyCode(PaymentLogSPI paymentLogSPI){
        if("KE".equals(paymentLogSPI.getSender().getCountryCode())){
            return "KES";
        }else if("UG".equals(paymentLogSPI.getSender().getCountryCode())){
            return "UGX";
        }else if("RW".equals(paymentLogSPI.getSender().getCountryCode())){
            return "RWF";
        }else if("TZ".equals(paymentLogSPI.getSender().getCountryCode())){
            return "TZS";
        }else if("SS".equals(paymentLogSPI.getSender().getCountryCode())){
            return "SSP";
        } else if("CD".equals(paymentLogSPI.getSender().getCountryCode())){
            return "CDF";
        }
        return "";
    }
    public String leftpad(float n, int padding) {
        return String.format("%0" + padding + "d", new Object[] { Float.valueOf(n) });
    }
    public String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", new Object[] { s });
    }

    public String padString(String s, int n, char c, boolean paddingLeft) {
        if (s == null) {
            s = " ";
        }
        StringBuilder str = new StringBuilder(s);
        int strLength = str.length();
        if (n > 0 && n > strLength) {
            for (int i = 0; i <= n; i++) {
                if (paddingLeft) {
                    if (i < n - strLength);
                } else if (i > strLength) {
                    str.append(c);
                }
            }
        }

        return str.toString();
    }
    private String getTranIdFrom(String respField) {
        if (respField == null) return null;
        String tranID = null;
        if (respField.contains("|")) {
            String[] fields = respField.split("\\|");
            if (fields.length > 1) {
                if (fields[0].length() > 3) {
                    tranID = fields[0];
                } else {
                    tranID = fields[1];
                }
            } else {
                tranID = respField;
            }
        } else {
            tranID = respField;
        }
        if (tranID != null) {
            return tranID.substring(0, Math.min(tranID.length(), 12));
        }
        return null;

    }
}
