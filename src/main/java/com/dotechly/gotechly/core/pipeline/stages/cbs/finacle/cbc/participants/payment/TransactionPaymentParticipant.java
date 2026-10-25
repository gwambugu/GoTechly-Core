package com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.participants.payment;


import com.dotechly.gotechly.core.GoTechlyCoreApplication;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.tm.bean.AppQBean;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsResponseCodes;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.Stan;
import com.dotechly.gotechly.core.service.impl.AppConfigsService;
import com.dotechly.gotechly.spi.service.dto.payment.CBS;
import com.dotechly.gotechly.spi.service.dto.payment.PaymentLogSPI;
import com.dotechly.gotechly.core.pipeline.stages.cbs.finacle.cbc.util.CbsExchange;
import com.dotechly.gotechly.spi.service.util.common.SerializeUtil;
import com.dotechly.gotechly.spi.service.util.common.SignatureUtil;
import org.apache.commons.codec.binary.Base64;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


import static org.apache.commons.lang3.StringUtils.isNumeric;

public class TransactionPaymentParticipant implements TransactionParticipant, Configurable {
    private final static Logger log = LoggerFactory.getLogger(TransactionPaymentParticipant.class);
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

        PaymentLogSPI paymentLogSPI = CbsExchange.get(ctx, "CBC_PAYMENT_REQUEST");
        if (paymentLogSPI == null) {
            log.error("TransactionPaymentParticipant: no CBC_PAYMENT_REQUEST in context");
            return;
        }
        try {
            ISOMsg finRequest = getISORequest(paymentLogSPI);
            String countryCode = paymentLogSPI.getSender() == null
                    ? null : paymentLogSPI.getSender().getCountryCode();
            String muxStr = AppQBean.getNextMux(countryCode);
            Map<String, MUX> muxMap = AppQBean.getMuxMap(countryCode);
            MUX mx = muxMap == null ? null : muxMap.get(muxStr);
            if (finRequest == null || mx == null) {
                // Nothing was sent. Report it as a timeout-shaped unknown rather than leaving the
                // stage to wait out its space timeout on a reply that will never come.
                log.error("Payment not sent (finRequest={}, mux={}) for requestId '{}'",
                        finRequest != null, mx != null,
                        paymentLogSPI.getDetails() == null ? "unknown" : paymentLogSPI.getDetails().getRequestId());
                paymentLogSPI = setAdditionalFieldsFromResponse(paymentLogSPI, Boolean.FALSE, null, null);
            } else {
                ISOMsg resp = mx.request(finRequest, 10000L);
                ISOMsg chargeResp = null;
                Boolean hasChargeRequest = hasSeparateChargeRequest(paymentLogSPI);
                if (hasChargeRequest
                        && resp != null
                        && "000".equalsIgnoreCase(resp.getString(39))) {
                    chargeResp = appChargeRequestIfPresent(resp, paymentLogSPI, mx);
                }
                paymentLogSPI = setAdditionalFieldsFromResponse(paymentLogSPI, hasChargeRequest, resp, chargeResp);
            }
        } catch (ISOException | RuntimeException e) {
            // The posting may or may not have reached Finacle, so this is TIMEOUT (unknown), never a
            // definitive failure. Field 37 is already stamped on the log, so the status inquiry can
            // still find out which it was.
            log.error("Acc Payment Participant==> {}", e.getMessage(), e);
            paymentLogSPI = setAdditionalFieldsFromResponse(paymentLogSPI, Boolean.FALSE, null, null);
        }
        // Unconditional: the stage blocks on this key, and an absent reply costs it the full space
        // timeout on top of whatever time the MUX already spent.
        CbsExchange.put(ctx, "CBC_PAYMENT_RESPONSE", paymentLogSPI);
    }

    private ISOMsg appChargeRequestIfPresent(ISOMsg resp, PaymentLogSPI paymentLogSPI, MUX mx) {
        AppConfigsService appConfigsService = GoTechlyCoreApplication.applicationContext.getBean(AppConfigsService.class);
        if (resp != null
                && "000".equals(resp.getString(39))
                && paymentLogSPI != null
                && paymentLogSPI.getCbs() != null
                && paymentLogSPI.getCbs().getAdditionalFields() != null
                && paymentLogSPI.getCbs().getAdditionalFields().get("cbc.iso.charge.request.hash") != null
                && paymentLogSPI.getCbs().getAdditionalFields().get("cbc.iso.charge.request") != null) {
            String isIsoRequestHash = paymentLogSPI.getCbs().getAdditionalFields().get("cbc.iso.charge.request.hash");
            Map<String, String> globalConfigs = appConfigsService.findByConfigIdPrefix("core.");
            byte[] digitalSignature = Base64.decodeBase64(isIsoRequestHash.getBytes(StandardCharsets.UTF_8));
            String clearRequest = paymentLogSPI.getCbs().getAdditionalFields().get("cbc.iso.charge.request");
            Boolean isSigned = SignatureUtil.verifySig(globalConfigs, clearRequest.getBytes(), digitalSignature);
            if (isSigned) {
                ISOMsg chargeRequest = (ISOMsg) SerializeUtil.strToObj(new String(clearRequest));
                try {
                    ISOMsg chargeResp = mx.request(chargeRequest, 10000L);
                    return chargeResp;
                } catch (ISOException e) {
                    log.error(" ISOException==> {}", e.getMessage());
                }
                return chargeRequest;
            }
        }
        return null;
    }

    private Boolean hasSeparateChargeRequest(PaymentLogSPI paymentLogSPI) {
        if (paymentLogSPI.getCbs() == null || paymentLogSPI.getCbs().getAdditionalFields() == null) {
            return false;
        }
        AppConfigsService appConfigsService = GoTechlyCoreApplication.applicationContext.getBean(AppConfigsService.class);
        String isIsoRequestHash = paymentLogSPI.getCbs().getAdditionalFields().get("cbc.iso.charge.request.hash");
        String clearRequest = paymentLogSPI.getCbs().getAdditionalFields().get("cbc.iso.charge.request");
        if (isIsoRequestHash != null && clearRequest != null) {
            Map<String, String> globalConfigs = appConfigsService.findByConfigIdPrefix("core.");
            byte[] digitalSignature = Base64.decodeBase64(isIsoRequestHash.getBytes(StandardCharsets.UTF_8));
            Boolean isSigned = SignatureUtil.verifySig(globalConfigs, clearRequest.getBytes(), digitalSignature);
            if (isSigned) {
                ISOMsg chargeRequest = (ISOMsg) SerializeUtil.strToObj(new String(clearRequest));
                log.info("chargeRequest present {}", chargeRequest.getString(37));
                return true;
            }
        }
        return false;
    }

    public ISOMsg getISORequest(PaymentLogSPI paymentLogSPI) {
        ISOMsg request = null;
        var cbs = paymentLogSPI.getCbs();
        var fields = (cbs != null) ? cbs.getAdditionalFields() : null;
        String isIsoRequestPresent = (fields != null) ? fields.get("cbc.iso.request.present") : null;
        if ("true".equalsIgnoreCase(isIsoRequestPresent)) {
            log.info("isoRequestPresent is true");
            if (paymentLogSPI.getCbs().getRequest() != null) {
                log.info("paymentLogSPI.getCbs().getRequest() !=null");
                String isIsoRequestHash = (fields != null) ? fields.get("cbc.iso.request.hash") : null;
                if (isIsoRequestHash != null) {
                    log.info("IsoRequestHash !=null");
                    AppConfigsService appConfigsService = GoTechlyCoreApplication.applicationContext.getBean(AppConfigsService.class);
                    Map<String, String> globalConfigs = appConfigsService.findByConfigIdPrefix("core.");
                    byte[] digitalSignature = Base64.decodeBase64(isIsoRequestHash.getBytes(StandardCharsets.UTF_8));
                    byte[] clearRequest = paymentLogSPI.getCbs().getRequest();
                    boolean isSigned = SignatureUtil.verifySig(globalConfigs, clearRequest, digitalSignature);
                    if (isSigned) {
                        request = (ISOMsg) SerializeUtil.strToObj(new String(clearRequest));
                        updateCBSdetails(request,paymentLogSPI);
                        return request;
                    } else {
                        log.error("isoRequestHash verification failed");
                    }
                } else {
                    log.error("isoRequestHash is null");
                }

            } else {
                log.error("paymentLogSPI.getCbs().getRequest()  is null");
            }
        } else {
            request = generateFundTransferRequest(paymentLogSPI);
            updateCBSdetails(request,paymentLogSPI);
        }
        return request;
    }

    private void updateCBSdetails(ISOMsg request,PaymentLogSPI paymentLogSPI) {
        if(request !=null){
            if(paymentLogSPI.getCbs()==null){
                paymentLogSPI.setCbs(new CBS());
            }
            if(paymentLogSPI.getCbs().getDrAcc()==null){
                paymentLogSPI.getCbs().setDrAcc(request.getString(102));
            }
            if(paymentLogSPI.getCbs().getCrAcc()==null){
                paymentLogSPI.getCbs().setCrAcc(request.getString(103));
            }
            // Stamp the outgoing fields on BOTH construction paths. generateFundTransferRequest does
            // this for messages it builds, but a pre-signed ISO request supplied by the caller
            // (cbc.iso.request.present=true) used to go out unrecorded — and field 37 is the only
            // handle the status inquiry has once the posting times out with no response to carry a
            // tranRef. Missing it means an unconfirmed transaction can never be reconciled.
            setAdditionalFieldsFromRequest(paymentLogSPI, request);
        }
    }

    public ISOMsg generateFundTransferRequest(PaymentLogSPI paymentLogSPI) {
        log.info("generateISOFundTransferWithChargeMsg");
        try {
            if (paymentLogSPI.getCbs() == null) {
                paymentLogSPI.setCbs(new CBS());
                paymentLogSPI.getCbs().setDrAcc(paymentLogSPI.getSender().getAccountNumber());
                paymentLogSPI.getCbs().setCrAcc(paymentLogSPI.getReceiver().getAccountNumber());
            }
            String senderName = paymentLogSPI.getSender().getSenderName();

            if (senderName.length() <= 8) {
                senderName = padRight(senderName, 10);
                senderName = padString(senderName, 10, '*', false);
                System.out.println(senderName + "| len=" + senderName.length());
            } else {
                senderName = padRight(senderName, 10);
                senderName = padString(senderName, 10, 'x', false);
            }

            String debitAccount = paymentLogSPI.getCbs().getDrAcc();
            String creditAccount = paymentLogSPI.getCbs().getCrAcc();
            String hRemarks = paymentLogSPI.getDetails().getRequestId();
            ISOMsg m = new ISOMsg();
            // Six digits from this instance's own block of the STAN space, so no other Core
            // instance can produce the same value. The RRN below is built from it, and the mux keys
            // on (DE12, DE37) - a shared STAN in the same second crosses core-banking responses
            // between transactions. See StanGenerator.
            String stan = Stan.next();
            log.info("create Fund Transfers Msg Method(detailsFundTransferMsg)");
            Date today = new Date();
            String tranDateTime = String.format("%tm%td%tH%tM%tS", today, today, today, today, today);
            String localTranDateTime = String.format("%ty%tm%td%tH%tM%tS", today, today, today, today, today, today);
            BigDecimal ammount = paymentLogSPI.getDetails().getAmount(); //new BigDecimal(transactionsLogDTO.getTransferAmount());
            BigDecimal tranamountFloat = ammount.multiply(new BigDecimal("100"));
            tranamountFloat = tranamountFloat.setScale(2, RoundingMode.CEILING);
            long tranAmount = tranamountFloat.longValue();
            String tranAmountstr = leftpad(tranAmount, 12);
            String rrn = stan + String.format("%tH%tM%tS", today, today, today);
            String origRRN = paymentLogSPI.getDetails().getRequestId().replaceAll("[a-zA-Z]", "");
            if (isNumeric(origRRN) && origRRN.length() >= 12) {
                rrn = origRRN.substring(origRRN.length() - 12);
            } else {
                rrn = leftpad(Long.parseLong(rrn), 12);
            }
            rrn = leftpad(Long.parseLong(rrn), 12);
            int remarksLen = (hRemarks.length() < 40) ? hRemarks.length() : 39;
            hRemarks = hRemarks.substring(0, remarksLen);
            String f42 = paymentLogSPI.getDetails().getRequestId();
            if (f42.length() > 15) {
                f42 = f42.substring(0, 14);
            }
            //            String f125 = transactionsLogDTO.getTransactionRef() + " " + senderName + " " + hRemarks;

            String f43 = paymentLogSPI.getDetails().getRequestId();
            int f43Len = (f43.length() < 40) ? f43.length() : 39;
            f43 = f43.substring(0, f43Len);
            String tranDebitNarration = (paymentLogSPI.getSender().getNarration() == null) ? "" : paymentLogSPI.getSender().getNarration();
            String tranCreditNarration = (paymentLogSPI.getReceiver().getNarration() == null) ? "" : paymentLogSPI.getReceiver().getNarration();
            String chargeDebitNarration = tranDebitNarration;
            String chargeCreditNarration = tranCreditNarration;
            String chargeDrAcc = paymentLogSPI.getCbs().getDrAcc();
            String chargeCrAcc = null;
            String rateCode = "CRC";
            String f123 = "GoTechly";

            String f125 = senderName + " " + hRemarks;
            f125 = tranDebitNarration + "|" + tranCreditNarration + "|" + chargeDebitNarration + "|" + chargeCreditNarration;
            String chargeCode = f125.contains("EFT") ? "EAZYBIZEFT" : "EAZYBIZ";
            if (paymentLogSPI.getCbs().getAdditionalFields() != null) {
                f123 = (paymentLogSPI.getCbs().getAdditionalFields().get("cbc.channel") != null) ? paymentLogSPI.getCbs().getAdditionalFields().get("cbc.channel") : "GoTechly";
                rateCode = (paymentLogSPI.getCbs().getAdditionalFields().get("rateCode") != null) ? paymentLogSPI.getCbs().getAdditionalFields().get("rateCode") : rateCode;
                chargeDrAcc = (paymentLogSPI.getCbs().getAdditionalFields().get("chargeDrAcc") != null) ? paymentLogSPI.getCbs().getAdditionalFields().get("chargeDrAcc") : paymentLogSPI.getCbs().getAdditionalFields().get("chargeDrAcc");
                chargeCrAcc = (paymentLogSPI.getCbs().getAdditionalFields().get("chargeCrAcc") != null) ? paymentLogSPI.getCbs().getAdditionalFields().get("chargeCrAcc") : null;
                chargeDebitNarration = (paymentLogSPI.getCbs().getAdditionalFields().get("chargeDrNarration") != null) ? paymentLogSPI.getCbs().getAdditionalFields().get("chargeDrNarration") : tranDebitNarration;
                chargeCreditNarration = (paymentLogSPI.getCbs().getAdditionalFields().get("chargeCrNarration") != null) ? paymentLogSPI.getCbs().getAdditionalFields().get("chargeCrNarration") : tranCreditNarration;
                chargeCode = (paymentLogSPI.getCbs().getAdditionalFields().get("chargeCode") != null) ? paymentLogSPI.getCbs().getAdditionalFields().get("chargeCode") : chargeCode;
            }


            String processCode = "400000";

            String f63 = null;
            if (chargeDrAcc != null && chargeCrAcc != null) {
                f63 = chargeDrAcc + "|" + chargeCrAcc;
            } else if (chargeCrAcc != null) {
                f63 = chargeCrAcc;
            }


            m.setMTI("1200");
            m.set(2, "4478150000000000");
            m.set(3, processCode);
            m.set(4, tranAmountstr);
            m.set(7, tranDateTime);
            m.set(11, stan);
            m.set(12, localTranDateTime);
            m.set(17, String.format("%tm%td", today, today));
            m.set(24, "200");
            m.set(32, "627851");
            m.set(35, "4478150012080766");
            m.set(37, rrn);
            m.set(41, rateCode);
            m.set(42, f42);
            m.set(43, f43);
            if (paymentLogSPI.getDetails().getChargeAmount() != null) {
                BigDecimal fees = paymentLogSPI.getDetails().getChargeAmount();
                BigDecimal charge = fees.multiply(new BigDecimal("100"));
                m.set(
                        46,
                        "00" +
                                paymentLogSPI.getDetails().getChargeCurrencyCode() +
                                "D" +
                                leftpad(charge.longValue(), 16) +
                                "00000001D" +
                                leftpad(charge.setScale(2, RoundingMode.CEILING).longValue() + tranAmount, 16) +
                                paymentLogSPI.getReceiver().getCurrencyCode()
                );
            }
            if (f125.contains("EFT")) {
                log.info("f125 contains EFT.");
                BigDecimal fees = paymentLogSPI.getDetails().getChargeAmount();
                BigDecimal charge = fees.multiply(new BigDecimal("100"));
                String f46 =
                        "00" +
                                paymentLogSPI.getDetails().getChargeCurrencyCode() +
                                "D" +
                                leftpad(charge.longValue(), 16) +
                                "00000000D" +
                                leftpad(charge.longValue(), 16) +
                                paymentLogSPI.getReceiver().getCurrencyCode();
                log.info("f46: " + f46);
                m.set(46, f46);
            }
            m.set(49, paymentLogSPI.getReceiver().getCurrencyCode());
            BigDecimal fees = paymentLogSPI.getDetails().getChargeAmount();
            if (fees.longValue() > 0) {
                String countryCode = paymentLogSPI.getSender().getCountryCode() == null ? "" : paymentLogSPI.getSender().getCountryCode();
                if (countryCode.equals("CD")) {
                    m.set(62, f63);
                } else {
                    m.set(63, f63);
                }
            }
            m.set(102, debitAccount.trim());
            m.set(103, creditAccount);
            m.set(123, f123);
            m.set(125, f125);
            m.set(126, chargeCode);
            log.info("detailsFundTransferWithChargeMsg is OK ISO==>" + m.getString(37));
            paymentLogSPI.getCbs().setRequest(SerializeUtil.objToStr((Serializable) m));
            paymentLogSPI = setAdditionalFieldsFromRequest(paymentLogSPI, m);
            return m;
        } catch (ISOException ex) {
            log.error("detailsFundTransferWithChargeMsg ISOException :" + ex.getMessage());
            ex.printStackTrace(System.out);
            return null;
        } catch (Exception ex) {
            log.error("detailsFundTransferWithChargeMsg Exception :" + ex.getMessage());
            ex.printStackTrace(System.out);
            return null;
        }
    }

    private PaymentLogSPI setAdditionalFieldsFromRequest(PaymentLogSPI paymentLogSPI, ISOMsg request) {
        if (paymentLogSPI.getCbs().getAdditionalFields() == null) {
            paymentLogSPI.getCbs().setAdditionalFields(new HashMap<>());
        }
        if (request != null) {
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE11", request.getString(11));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE37", request.getString(37));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE42", request.getString(42));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE43", request.getString(43));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE46", request.getString(46));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE49", request.getString(49));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE102", request.getString(102));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE103", request.getString(103));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE123", request.getString(123));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE125", request.getString(125));
            paymentLogSPI.getCbs().getAdditionalFields().put("cbc.payment.request.DE126", request.getString(126));
        }

        return paymentLogSPI;
    }

    private PaymentLogSPI setAdditionalFieldsFromResponse(
            PaymentLogSPI paymentLogSPI,
            Boolean hasSeparateCharge,
            ISOMsg principalResponse,
            ISOMsg chargeResponse) {

        if (paymentLogSPI == null || paymentLogSPI.getCbs() == null) {
            return paymentLogSPI;
        }

        if (paymentLogSPI.getCbs().getAdditionalFields() == null) {
            paymentLogSPI.getCbs().setAdditionalFields(new HashMap<>());
        }

        boolean separateCharge = Boolean.TRUE.equals(hasSeparateCharge);
        Map<String, String> additionalFields =
                paymentLogSPI.getCbs().getAdditionalFields();
        Map<String, String> receiverAdditionalFields =
                paymentLogSPI.getReceiver().getAdditionalFields();
        if(receiverAdditionalFields==null){
            receiverAdditionalFields = new HashMap<>();
        }

        if (principalResponse == null) {
            // Nothing came back, so this attempt has no response code of its own â and the CBS block
            // we were handed may already carry one from an earlier stage. The deferred reconciler
            // runs cbc-txn-status and then hands cbc-txn-payment the very same PaymentLogSPI the
            // gate enriched, so a repost that times out arrives here holding the *inquiry's* answer:
            // respCode "000(F)", respDesc "Transaction Not found". Left in place,
            // TransactionPaymentStage classifies this attempt off that stale code â CbsResponseCodes
            // reads "000(F)" as unrecognised, hence DECLINED â and halts cbc-payment-failed, which is
            // a terminal halt code, so the reconciler escalates the row to NEEDS_ATTENTION on its
            // first attempt instead of rescheduling it. Clearing is the point of this branch, not
            // tidiness: an absent code is what makes the stage read the timeout as
            // cbc-payment-unconfirmed.
            CBS cbs = paymentLogSPI.getCbs();
            cbs.setStatus("TIMEOUT");
            cbs.setRespCode(null);
            cbs.setRespDesc(null);
            return paymentLogSPI;
        }

        String principalRespCode = principalResponse.getString(39);

        if (!separateCharge) {
            paymentLogSPI.getCbs().setRespCode(principalRespCode);

            String tranId = getTranIdFrom(principalResponse.getString(127));
            if (tranId != null) {
                paymentLogSPI.getCbs().setTranRef(tranId);
            }

            String balance = getBalanceFrom(principalResponse.getString(127));
            if (balance != null) {
                receiverAdditionalFields.put(
                        "cbs.balance",
                        balance
                );
            }

            String mobileNumber = getMobileNumberFrom(principalResponse.getString(127));
            if (mobileNumber != null) {
                receiverAdditionalFields.put(
                        "cbs.mobileNumber",
                        mobileNumber
                );
            }

                paymentLogSPI.getReceiver().setAdditionalFields(receiverAdditionalFields);
            additionalFields.put(
                    "cbc.payment.response.DE39",
                    principalRespCode
            );
            additionalFields.put(
                    "cbc.payment.response.DE37",
                    principalResponse.getString(37)
            );

            if (tranId != null) {
                additionalFields.put(
                        "cbc.payment.response.DE127",
                        tranId
                );
            }

            // Same classifier the stage uses. These two must agree: TransactionPaymentStage checks
            // "POSTED".equals(status) before it looks at DE39, so a participant that calls a timeout
            // FAILED while the stage calls it unconfirmed sends the transaction down the branch that
            // reports a disagreement instead of the one that reconciles it.
            paymentLogSPI.getCbs().setStatus(switch (CbsResponseCodes.classify(principalRespCode)) {
                case SUCCESS -> "POSTED";
                case TIMEOUT -> "TIMEOUT";
                case DECLINED -> "FAILED";
            });

            return paymentLogSPI;
        }

        // Separate charge
        if (chargeResponse == null) {
            log.info("CHARGE Response Timeout..");
            // Same staleness rule as the principal-timeout branch above: this path returns before
            // reaching any setRespCode, so stamp the principal leg's own code rather than leaving
            // whatever an earlier stage wrote on this CBS block.
            paymentLogSPI.getCbs().setRespCode(principalRespCode);
            paymentLogSPI.getCbs().setRespDesc(null);
            paymentLogSPI.getCbs().setStatus("CHARGE-TIMEOUT");
            return paymentLogSPI;
        }

        String chargeRespCode = chargeResponse.getString(39);
        String chargeTranId = getTranIdFrom(chargeResponse.getString(127));

        additionalFields.put(
                "cbc.payment.charge.response.DE39",
                chargeRespCode
        );
        additionalFields.put(
                "cbc.payment.charge.response.DE37",
                chargeResponse.getString(37)
        );

        if (chargeTranId != null) {
            additionalFields.put(
                    "cbc.payment.charge.response.DE127",
                    chargeTranId
            );
        }

        CbsResponseCodes.Verdict principalVerdict = CbsResponseCodes.classify(principalRespCode);
        if (principalVerdict == CbsResponseCodes.Verdict.SUCCESS) {
            // The principal posted; only the charge leg is in question.
            paymentLogSPI.getCbs().setStatus(switch (CbsResponseCodes.classify(chargeRespCode)) {
                case SUCCESS -> "POSTED";
                case TIMEOUT -> "CHARGE-TIMEOUT";
                case DECLINED -> "CHARGE-FAILED";
            });
        } else if (principalVerdict == CbsResponseCodes.Verdict.TIMEOUT) {
            paymentLogSPI.getCbs().setStatus("TIMEOUT");
        } else {
            paymentLogSPI.getCbs().setStatus("FAILED");
        }

        return paymentLogSPI;
    }

    public String leftpad(long n, int padding) {
        return String.format("%0" + padding + "d", new Object[]{Long.valueOf(n)});
    }

    public String getCurrencyCode(PaymentLogSPI paymentLogSPI) {
        if ("KE".equals(paymentLogSPI.getSender().getCountryCode())) {
            return "KES";
        } else if ("UG".equals(paymentLogSPI.getSender().getCountryCode())) {
            return "UGX";
        } else if ("RW".equals(paymentLogSPI.getSender().getCountryCode())) {
            return "RWF";
        } else if ("TZ".equals(paymentLogSPI.getSender().getCountryCode())) {
            return "TZS";
        } else if ("SS".equals(paymentLogSPI.getSender().getCountryCode())) {
            return "SSP";
        } else if ("CD".equals(paymentLogSPI.getSender().getCountryCode())) {
            return "CDF";
        }
        return "";
    }

    public String leftpad(float n, int padding) {
        return String.format("%0" + padding + "d", new Object[]{Float.valueOf(n)});
    }

    public String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", new Object[]{s});
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
                    if (i < n - strLength) ;
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
    private String getBalanceFrom(String respField) {
        if (respField == null) return null;
        String balance = null;
        if (respField.contains("|")) {
            String[] fields = respField.split("\\|");
            if (fields.length >= 2) {
                balance = fields[1];
            } else {
                balance = null;
            }
        } else {
            balance = null;
        }
        if (balance != null) {
            return balance.substring(0, Math.min(balance.length(), 12));
        }
        return null;

    }
    private String getMobileNumberFrom(String respField) {
        if (respField == null) return null;
        String mobile = null;
        if (respField.contains("|")) {
            String[] fields = respField.split("\\|");
            if (fields.length >= 3) {
                mobile = fields[2];
            } else {
                mobile = null;
            }
        } else {
            mobile = null;
        }
        if (mobile != null) {
            return mobile.substring(0, Math.min(mobile.length(), 12));
        }
        return null;

    }
}
