package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.participants.validate;


import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
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
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isNumeric;

public class AccountValidationParticipant implements TransactionParticipant, Configurable {
    private static Logger log = LoggerFactory.getLogger(AccountValidationParticipant.class);
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

        PaymentLogSPI paymentLogSPI = CbsExchange.get(ctx, "CBC_ACC_VALIDATION_REQUEST");
        log.info("commit Transaction {}");
        ISOMsg finRequest = getAccountInquireMsg(paymentLogSPI);
        String muxStr = AppQBean.getNextMux(paymentLogSPI.getSender().getCountryCode());
        Map<String, MUX> muxMap = AppQBean.getMuxMap(paymentLogSPI.getSender().getCountryCode());
        MUX mx = muxMap.get(muxStr);
        try {
            ISOMsg resp = mx.request(finRequest, 10000L);
            paymentLogSPI  = getFinacleNames(paymentLogSPI,resp);
            CbsExchange.put(ctx, "CBC_ACC_VALIDATION_RESPONSE", paymentLogSPI);
        } catch (ISOException e) {
           log.error("Acc Validate Participant==> {}",e.getMessage());
        }

    }
    private PaymentLogSPI getFinacleNames( PaymentLogSPI paymentLogSPI, ISOMsg finResponse) {
        String f39 = finResponse.getString(39);
        String f125 = finResponse.getString(125) ;
        String f127 = finResponse.getString(127);
        if(paymentLogSPI.getCbs()==null) {
            paymentLogSPI.setCbs(new CBS());
        }
        paymentLogSPI.getCbs().getAdditionalFields().put("validation.response.DE39",f39);
        if (f39 == null) return paymentLogSPI;
        if (!f39.equals("000") && f127 == null) return paymentLogSPI;
        String theData = f125;
        if (f127 != null &&
                f127.contains("CSIS:")) {
            theData = f127;
            theData = theData.replaceAll("CSIS:", "");
        }

        System.out.println("theData=" + theData);
        String[] data = theData.split("/");
        String name = (data != null) ? data[0] : "";
        String[] namelist = name.split(" ");
        if (namelist != null) {

            if (namelist.length > 1) {
                String fName = namelist[0];
                String oName = namelist[1];
                //CIS 54311217141                     DOMINIC ECHEBUCHE LITUNYA                                                       DOMINIC ECHEBUCHE LITUNY
                fName = fName.trim();
                if (isNumeric(fName)) {
                    fName = namelist[1];
                    oName = namelist[2];
                }
                paymentLogSPI.getReceiver().setReceiverName(fName);
                if (isNumeric(fName)) {
                    if (namelist.length > 2){
                        oName = oName + " " + namelist[3];
                    }
                }else{
                    if (namelist.length > 2){
                        oName = oName + " " + namelist[2];
                    }
                }
                paymentLogSPI.getReceiver().setReceiverName(fName + " " + oName);
            } else {
                paymentLogSPI.getReceiver().setReceiverName(name);
            }
            return paymentLogSPI;
        }
        return null;
    }

    public ISOMsg getAccountInquireMsg(PaymentLogSPI paymentLogSPI) {
        try {
//            if (account.length() > 15) {
//                account = account.substring(0, 15);
//            }
            ISOMsg m = new ISOMsg();
            // Was random.nextInt(1_000_000): by the birthday bound a single instance had roughly
            // even odds of some collision within ~1,200 messages, before any second instance was
            // involved. See StanGenerator.
            String stan = Stan.next();
            System.out.println("create enquireMsg Msg Method");
            Date today = new Date();
            String tranDateTime = String.format("%tm%td%tH%tM%tS", new Object[]{today, today, today, today, today});
            String localTranDateTime = String.format("%ty%tm%td%tH%tM%tS", new Object[]{today, today, today, today, today, today});
            String rrn = stan + String.format("%tH%tM%tS", new Object[]{today, today, today});
            rrn = leftpad(Long.valueOf(rrn).longValue(), 12);
            String f123 = "GoTechly";

            if((paymentLogSPI.getCbs().getAdditionalFields())!=null) {
                if (paymentLogSPI.getCbs().getAdditionalFields().containsKey("status.request.DE123")) {
                    f123 = paymentLogSPI.getCbs().getAdditionalFields().get("status.request.DE123");
                }
            }
            m.setMTI("1200");
            m.set(2, "6278519999011712");
            m.set(3, "820000");
            m.set(7, tranDateTime);
            m.set(11, stan);
            m.set(12, localTranDateTime);
            m.set(17, String.format("%tm%td", new Object[]{today, today}));
            m.set(24, "200");
            m.set(32, "111111");
            m.set(35, "6278519999011712");
            m.set(37, rrn);
            m.set(41, "CAMPAIGN");
            m.set(43, "GoTechly");
            m.set(102, paymentLogSPI.getReceiver().getAccountNumber());
            m.set(123, f123);
            m.set(126, "CAMPAIGN");
            return m;
        } catch (ISOException ex) {
            log.error("enquireAccFromFinMsg Exception", ex.getMessage());
            return null;
        }
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
}
