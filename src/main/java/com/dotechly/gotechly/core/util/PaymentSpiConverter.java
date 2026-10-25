package com.dotechly.gotechly.core.util;

import com.dotechly.gotechly.core.service.dto.PaymentLogDTO;
import com.dotechly.gotechly.spi.service.dto.payment.*;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;

@Component
public class PaymentSpiConverter {

    public PaymentLogDTO toDto(PaymentLogSPI spi) {
        if (spi == null) {
            return null;
        }
        PaymentLogDTO dto = new PaymentLogDTO();
        dto.setId(spi.getId());

        // Details mapping
        Details details = spi.getDetails();
        if (details != null) {
            dto.setPaymentType(details.getPaymentType());
            dto.setRequestType(details.getRequestType());
            dto.setRequestId(details.getRequestId());
            dto.setRequestTimestamp(toInstant(details.getRequestTimestamp()));
            dto.setAmount(details.getAmount());
            dto.setCurrencyCode(details.getCurrencyCode());
            dto.setChargeAmount(details.getChargeAmount());
            dto.setChargeCurrencyCode(details.getChargeCurrencyCode());
            dto.setProcessingCode(details.getProcessingCode());
            dto.setStatus(details.getStatus());
            dto.setApiMode(details.getApiMode());
            dto.setResponseCode(details.getResponseCode());
            dto.setResponseDesc(details.getResponseDesc());
            dto.setResponseTimestamp(toInstant(details.getResponseTimestamp()));
            dto.setDetailsAdditionalFields(details.getAdditionalFields());
            
            dto.setAppInstanceId(details.getAppInstanceId());
            dto.setAppHost(details.getAppHost());
            dto.setSourceIp(details.getSourceIp());
            dto.setSourceSystem(details.getSourceSystem());
            dto.setSourceSystemCallBackUrl(details.getSourceSystemCallBackUrl());
            
            dto.setFreeText1(details.getFreeText1());
            dto.setFreeText2(details.getFreeText2());
            dto.setHash1(details.getHash1());
            dto.setHash2(details.getHash2());
            dto.setRecordVersion(details.getRecordVersion());
            dto.setCreatedAt(toInstant(details.getCreateAt()));
            dto.setUpdatedAt(toInstant(details.getUpdatedAt()));
            //STATUS INQUIRY
            dto.setStatusCount(details.getStatusCount());
            dto.setStatusResponseCode(details.getStatusResponseCode());
            dto.setStatusResponseDescription(details.getStatusResponseDescription());
        }

        // Sender mapping
        Sender sender = spi.getSender();
        if (sender != null) {
            dto.setSenderType(sender.getType());
            dto.setSenderAccount(sender.getAccountNumber());
            dto.setSenderCurrencyCode(sender.getCurrencyCode());
            dto.setSenderBusinessNumber(sender.getBusinessNumber());
            dto.setSenderTranCode(sender.getTranCode());
            dto.setSenderReference(sender.getReference());
            dto.setSenderMsisdn(sender.getMsisdn());
            dto.setSenderNarration(sender.getNarration());
            dto.setSenderSenderName(sender.getSenderName());
            dto.setSenderPartyId(sender.getPartyId());
            dto.setSenderCountryCode(sender.getCountryCode());
            dto.setSenderAdditionalFields(sender.getAdditionalFields());
        }

        // Receiver mapping
        Receiver receiver = spi.getReceiver();
        if (receiver != null) {
            dto.setReceiverType(receiver.getType());
            dto.setReceiverAccount(receiver.getAccountNumber());
            dto.setReceiverCurrencyCode(receiver.getCurrencyCode());
            dto.setReceiverBusinessNumber(receiver.getBusinessNumber());
            dto.setReceiverTranCode(receiver.getTranCode());
            dto.setReceiverReference(receiver.getReference());
            dto.setReceiverNarration(receiver.getNarration());
            dto.setReceiverMsisdn(receiver.getMsisdn());
            dto.setReceiverReceiverName(receiver.getReceiverName());
            dto.setReceiverPartyId(receiver.getPartyId());
            dto.setReceiverCountryCode(receiver.getCountryCode());
            dto.setReceiverAdditionalFields(receiver.getAdditionalFields());
        }

        // CBS mapping
        CBS cbs = spi.getCbs();
        if (cbs != null) {
            dto.setCbsRequestId(cbs.getRequestId());
            dto.setCbsReq(cbs.getRequest());
            dto.setCbsReqTime(toInstant(cbs.getReqTime()));
            dto.setCbsNarration(cbs.getNarration());
            dto.setCbsHost(cbs.getHost());
            dto.setCbsDrAcc(cbs.getDrAcc());
            dto.setCbsCrAcc(cbs.getCrAcc());
            dto.setTransactionCode(cbs.getTransactionCode());
            dto.setCbsStatus(cbs.getStatus());
            dto.setCbsApiMode(cbs.getApiMode());
            dto.setCbsRespCode(cbs.getRespCode());
            dto.setCbsRespDesc(cbs.getRespDesc());
            dto.setCbsTranRef(cbs.getTranRef());
            dto.setCbsRespTime(toInstant(cbs.getRespTime()));
            dto.setCbsRetryCount(cbs.getRetryCount());
            dto.setCbsRevRespCode(cbs.getRevRespCode());
            dto.setCbsRevRespDesc(cbs.getRevRespDesc());
            dto.setCbsAdditionalFields(cbs.getAdditionalFields());
        }

        // PSP mapping
        PSP psp = spi.getPsp();
        if (psp != null) {
            dto.setPspRequestID(psp.getRequestID());
            String pspId = (psp.getPspId()==null)?spi.getDetails().getPaymentType(): psp.getPspId();
            dto.setPspId(pspId);
            dto.setPspRequestTime(toInstant(psp.getRequestTime()));
            dto.setPspNarration(psp.getNarration());
            dto.setPspHostURL(psp.getHostURL());
            dto.setPspDrAccount(psp.getDrAccount());
            dto.setPspCrAccount(psp.getCrAccount());
            dto.setPspStatus(psp.getStatus());
            dto.setPspApiMode(psp.getApiMode());
            dto.setPspRequest(psp.getRequest() != null ? psp.getRequest().getBytes() : null);
            dto.setPspResponse(psp.getResponse() != null ? psp.getResponse().getBytes() : null);
            dto.setPspResponseTime(toInstant(psp.getResponseTime()));
            dto.setPspResponseCode(psp.getResponseCode());
            dto.setPspResponseDesc(psp.getResponseDesc());
            dto.setPspResult(psp.getResult() != null ? psp.getResult().getBytes() : null);
            dto.setPspResultCode(psp.getResultCode());
            dto.setPspResultDesc(psp.getResultDesc());
            dto.setPspReferenceCode(psp.getReferenceCode());
            dto.setPspResultTime(toInstant(psp.getResultTime()));
            dto.setPspRetryCount(psp.getRetryCount());
            dto.setPspRevRespCode(psp.getRevRespCode());
            dto.setPspRevRespDesc(psp.getRevRespDesc());
            dto.setPspAdditionalFields(psp.getAdditionalFields());
        }

        return dto;
    }

    public PaymentLogSPI toSpi(PaymentLogDTO dto) {
        if (dto == null) {
            return null;
        }
        PaymentLogSPI spi = new PaymentLogSPI();
        spi.setId(dto.getId());

        // Details
        Details details = new Details();
        details.setPaymentType(dto.getPaymentType());
        details.setRequestType(dto.getRequestType());
        details.setRequestId(dto.getRequestId());
        details.setRequestTimestamp(toTimestamp(dto.getRequestTimestamp()));
        details.setAmount(dto.getAmount());
        details.setCurrencyCode(dto.getCurrencyCode());
        details.setChargeAmount(dto.getChargeAmount());
        details.setChargeCurrencyCode(dto.getChargeCurrencyCode());
        details.setProcessingCode(dto.getProcessingCode());
        details.setStatus(dto.getStatus());
        details.setApiMode(dto.getApiMode());
        details.setResponseCode(dto.getResponseCode());
        details.setResponseDesc(dto.getResponseDesc());
        details.setResponseTimestamp(toTimestamp(dto.getResponseTimestamp()));
        details.setAdditionalFields(dto.getDetailsAdditionalFields());
        details.setAppInstanceId(dto.getAppInstanceId());
        details.setAppHost(dto.getAppHost());
        details.setSourceIp(dto.getSourceIp());
        details.setSourceSystem(dto.getSourceSystem());
        details.setSourceSystemCallBackUrl(dto.getSourceSystemCallBackUrl());
        details.setFreeText1(dto.getFreeText1());
        details.setFreeText2(dto.getFreeText2());
        details.setHash1(dto.getHash1());
        details.setHash2(dto.getHash2());
        details.setRecordVersion(dto.getRecordVersion());
        details.setCreateAt(toTimestamp(dto.getCreatedAt()));
        details.setUpdatedAt(toTimestamp(dto.getUpdatedAt()));
        details.setStatusCount(dto.getStatusCount());
        details.setStatusResponseCode(dto.getStatusResponseCode());
        details.setStatusResponseDescription(dto.getStatusResponseDescription());
        spi.setDetails(details);

        // Sender
        Sender sender = new Sender();
        sender.setType(dto.getSenderType());
        sender.setAccountNumber(dto.getSenderAccount());
        sender.setCurrencyCode(dto.getSenderCurrencyCode());
        sender.setBusinessNumber(dto.getSenderBusinessNumber());
        sender.setTranCode(dto.getSenderTranCode());
        sender.setReference(dto.getSenderReference());
        sender.setMsisdn(dto.getSenderMsisdn());
        sender.setNarration(dto.getSenderNarration());
        sender.setSenderName(dto.getSenderSenderName());
        sender.setPartyId(dto.getSenderPartyId());
        sender.setCountryCode(dto.getSenderCountryCode());
        sender.setAdditionalFields(dto.getSenderAdditionalFields());
        spi.setSender(sender);

        // Receiver
        Receiver receiver = new Receiver();
        receiver.setType(dto.getReceiverType());
        receiver.setAccountNumber(dto.getReceiverAccount());
        receiver.setCurrencyCode(dto.getReceiverCurrencyCode());
        receiver.setBusinessNumber(dto.getReceiverBusinessNumber());
        receiver.setTranCode(dto.getReceiverTranCode());
        receiver.setReference(dto.getReceiverReference());
        receiver.setNarration(dto.getReceiverNarration());
        receiver.setMsisdn(dto.getReceiverMsisdn());
        receiver.setReceiverName(dto.getReceiverReceiverName());
        receiver.setPartyId(dto.getReceiverPartyId());
        receiver.setCountryCode(dto.getReceiverCountryCode());
        receiver.setAdditionalFields(dto.getReceiverAdditionalFields());
        spi.setReceiver(receiver);

        // CBS
        CBS cbs = new CBS();
        cbs.setRequestId(dto.getCbsRequestId());
        cbs.setRequest(dto.getCbsReq());
        cbs.setReqTime(toTimestamp(dto.getCbsReqTime()));
        cbs.setNarration(dto.getCbsNarration());
        cbs.setHost(dto.getCbsHost());
        cbs.setDrAcc(dto.getCbsDrAcc());
        cbs.setCrAcc(dto.getCbsCrAcc());
        cbs.setTransactionCode(dto.getTransactionCode());
        cbs.setStatus(dto.getCbsStatus());
        cbs.setApiMode(dto.getCbsApiMode());
        cbs.setRespCode(dto.getCbsRespCode());
        cbs.setRespDesc(dto.getCbsRespDesc());
        cbs.setTranRef(dto.getCbsTranRef());
        cbs.setRespTime(toTimestamp(dto.getCbsRespTime()));
        cbs.setRetryCount(dto.getCbsRetryCount());
        cbs.setRevRespCode(dto.getCbsRevRespCode());
        cbs.setRevRespDesc(dto.getCbsRevRespDesc());
        cbs.setAdditionalFields(dto.getCbsAdditionalFields());
        spi.setCbs(cbs);

        // PSP
        PSP psp = new PSP();
        psp.setRequestID(dto.getPspRequestID());
        psp.setPspId(dto.getPspId());
        psp.setRequestTime(toTimestamp(dto.getPspRequestTime()));
        psp.setNarration(dto.getPspNarration());
        psp.setHostURL(dto.getPspHostURL());
        psp.setDrAccount(dto.getPspDrAccount());
        psp.setCrAccount(dto.getPspCrAccount());
        psp.setStatus(dto.getPspStatus());
        psp.setApiMode(dto.getPspApiMode());
        psp.setRequest(dto.getPspRequest() != null ? new String(dto.getPspRequest()) : null);
        psp.setResponse(dto.getPspResponse() != null ? new String(dto.getPspResponse()) : null);
        psp.setResponseTime(toTimestamp(dto.getPspResponseTime()));
        psp.setResponseCode(dto.getPspResponseCode());
        psp.setResponseDesc(dto.getPspResponseDesc());
        psp.setResult(dto.getPspResult() != null ? new String(dto.getPspResult()) : null);
        psp.setResultCode(dto.getPspResultCode());
        psp.setResultDesc(dto.getPspResultDesc());
        psp.setReferenceCode(dto.getPspReferenceCode());
        psp.setResultTime(toTimestamp(dto.getPspResultTime()));
        psp.setRetryCount(dto.getPspRetryCount());
        psp.setRevRespCode(dto.getPspRevRespCode());
        psp.setRevRespDesc(dto.getPspRevRespDesc());
        psp.setAdditionalFields(dto.getPspAdditionalFields());
        spi.setPsp(psp);

        return spi;
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp != null ? timestamp.toInstant() : null;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
