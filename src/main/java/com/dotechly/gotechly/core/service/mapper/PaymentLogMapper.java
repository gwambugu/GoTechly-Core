package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.PaymentLog;
import com.dotechly.gotechly.core.service.dto.PaymentLogDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PaymentLogMapper implements EntityMapper<PaymentLogDTO, PaymentLog> {

    @Override
    public PaymentLog toEntity(PaymentLogDTO dto) {
        if (dto == null) {
            return null;
        }

        PaymentLog paymentLog = new PaymentLog();

        paymentLog.setId(dto.getId());
        paymentLog.setPaymentType(dto.getPaymentType());
        paymentLog.setRequestType(dto.getRequestType());
        paymentLog.setRequestId(dto.getRequestId());
        paymentLog.setRequestTimestamp(dto.getRequestTimestamp());
        paymentLog.setAmount(dto.getAmount());
        paymentLog.setCurrencyCode(dto.getCurrencyCode());
        paymentLog.setChargeAmount(dto.getChargeAmount());
        paymentLog.setChargeCurrencyCode(dto.getChargeCurrencyCode());
        paymentLog.setProcessingCode(dto.getProcessingCode());
        paymentLog.setAppInstanceId(dto.getAppInstanceId());
        paymentLog.setAppHost(dto.getAppHost());
        paymentLog.setStatus(dto.getStatus());
        paymentLog.setApiMode(dto.getApiMode());
        paymentLog.setResponseCode(dto.getResponseCode());
        paymentLog.setResponseDesc(dto.getResponseDesc());
        paymentLog.setResponseTimestamp(dto.getResponseTimestamp());
        paymentLog.setSourceIp(dto.getSourceIp());
        paymentLog.setSourceSystem(dto.getSourceSystem());
        paymentLog.setSourceSystemCallBackUrl(dto.getSourceSystemCallBackUrl());
        paymentLog.setFreeText1(dto.getFreeText1());
        paymentLog.setFreeText2(dto.getFreeText2());
        paymentLog.setHash1(dto.getHash1());
        paymentLog.setHash2(dto.getHash2());
        paymentLog.setRecordVersion(dto.getRecordVersion());
        paymentLog.setCreatedAt(dto.getCreatedAt());
        paymentLog.setUpdatedAt(dto.getUpdatedAt());
        paymentLog.setSenderType(dto.getSenderType());
        paymentLog.setSenderAccount(dto.getSenderAccount());
        paymentLog.setSenderCurrencyCode(dto.getSenderCurrencyCode());
        paymentLog.setSenderBusinessNumber(dto.getSenderBusinessNumber());
        paymentLog.setSenderTranCode(dto.getSenderTranCode());
        paymentLog.setSenderReference(dto.getSenderReference());
        paymentLog.setSenderMsisdn(dto.getSenderMsisdn());
        paymentLog.setSenderNarration(dto.getSenderNarration());
        paymentLog.setSenderSenderName(dto.getSenderSenderName());
        paymentLog.setSenderPartyId(dto.getSenderPartyId());
        paymentLog.setSenderCountryCode(dto.getSenderCountryCode());
        paymentLog.setReceiverType(dto.getReceiverType());
        paymentLog.setReceiverAccount(dto.getReceiverAccount());
        paymentLog.setReceiverCurrencyCode(dto.getReceiverCurrencyCode());
        paymentLog.setReceiverBusinessNumber(dto.getReceiverBusinessNumber());
        paymentLog.setReceiverTranCode(dto.getReceiverTranCode());
        paymentLog.setReceiverReference(dto.getReceiverReference());
        paymentLog.setReceiverNarration(dto.getReceiverNarration());
        paymentLog.setReceiverMsisdn(dto.getReceiverMsisdn());
        paymentLog.setReceiverReceiverName(dto.getReceiverReceiverName());
        paymentLog.setReceiverPartyId(dto.getReceiverPartyId());
        paymentLog.setReceiverCountryCode(dto.getReceiverCountryCode());
        paymentLog.setCbsRequestId(dto.getCbsRequestId());
        paymentLog.setCbsReq(dto.getCbsReq());
        paymentLog.setCbsReqTime(dto.getCbsReqTime());
        paymentLog.setCbsNarration(dto.getCbsNarration());
        paymentLog.setCbsHost(dto.getCbsHost());
        paymentLog.setCbsDrAcc(dto.getCbsDrAcc());
        paymentLog.setCbsCrAcc(dto.getCbsCrAcc());
        paymentLog.setTransactionCode(dto.getTransactionCode());
        paymentLog.setCbsStatus(dto.getCbsStatus());
        paymentLog.setCbsApiMode(dto.getCbsApiMode());
        paymentLog.setCbsRespCode(dto.getCbsRespCode());
        paymentLog.setCbsRespDesc(dto.getCbsRespDesc());
        paymentLog.setCbsTranRef(dto.getCbsTranRef());
        paymentLog.setCbsRespTime(dto.getCbsRespTime());
                paymentLog.setCbsRetryCount(dto.getCbsRetryCount());
                paymentLog.setCbsRevRespCode(dto.getCbsRevRespCode());
                paymentLog.setCbsRevRespDesc(dto.getCbsRevRespDesc());
        
                paymentLog.setSenderAdditionalFields(dto.getSenderAdditionalFields());
                paymentLog.setReceiverAdditionalFields(dto.getReceiverAdditionalFields());
                paymentLog.setDetailsAdditionalFields(dto.getDetailsAdditionalFields());
                paymentLog.setCbsAdditionalFields(dto.getCbsAdditionalFields());
                paymentLog.setPspAdditionalFields(dto.getPspAdditionalFields());
        
                paymentLog.setPspRequestID(dto.getPspRequestID());
        
        paymentLog.setPspId(dto.getPspId());
        paymentLog.setPspRequestTime(dto.getPspRequestTime());
        paymentLog.setPspNarration(dto.getPspNarration());
        paymentLog.setPspHostURL(dto.getPspHostURL());
        paymentLog.setPspDrAccount(dto.getPspDrAccount());
        paymentLog.setPspCrAccount(dto.getPspCrAccount());
        paymentLog.setPspStatus(dto.getPspStatus());
        paymentLog.setPspApiMode(dto.getPspApiMode());
        paymentLog.setPspRequest(dto.getPspRequest());
        paymentLog.setPspResponse(dto.getPspResponse());
        paymentLog.setPspResponseTime(dto.getPspResponseTime());
        paymentLog.setPspResponseCode(dto.getPspResponseCode());
        paymentLog.setPspResponseDesc(dto.getPspResponseDesc());
        paymentLog.setPspResult(dto.getPspResult());
        paymentLog.setPspResultCode(dto.getPspResultCode());
        paymentLog.setPspResultDesc(dto.getPspResultDesc());
        paymentLog.setPspReferenceCode(dto.getPspReferenceCode());
        paymentLog.setPspResultTime(dto.getPspResultTime());
        paymentLog.setPspRetryCount(dto.getPspRetryCount());
        paymentLog.setPspRevRespCode(dto.getPspRevRespCode());
        paymentLog.setPspRevRespDesc(dto.getPspRevRespDesc());

        paymentLog.setStatusTimestamp(dto.getStatusTimestamp());
        paymentLog.setStatusCount(dto.getStatusCount());
        paymentLog.setStatusResponseCode(dto.getStatusResponseCode());
        paymentLog.setStatusResponseDescription(dto.getStatusResponseDescription());

        return paymentLog;
    }

    @Override
    public PaymentLogDTO toDto(PaymentLog entity) {
        if (entity == null) {
            return null;
        }

        PaymentLogDTO paymentLogDTO = new PaymentLogDTO();

        paymentLogDTO.setId(entity.getId());
        paymentLogDTO.setPaymentType(entity.getPaymentType());
        paymentLogDTO.setRequestType(entity.getRequestType());
        paymentLogDTO.setRequestId(entity.getRequestId());
        paymentLogDTO.setRequestTimestamp(entity.getRequestTimestamp());
        paymentLogDTO.setAmount(entity.getAmount());
        paymentLogDTO.setCurrencyCode(entity.getCurrencyCode());
        paymentLogDTO.setChargeAmount(entity.getChargeAmount());
        paymentLogDTO.setChargeCurrencyCode(entity.getChargeCurrencyCode());
        paymentLogDTO.setProcessingCode(entity.getProcessingCode());
        paymentLogDTO.setAppInstanceId(entity.getAppInstanceId());
        paymentLogDTO.setAppHost(entity.getAppHost());
        paymentLogDTO.setStatus(entity.getStatus());
        paymentLogDTO.setApiMode(entity.getApiMode());
        paymentLogDTO.setResponseCode(entity.getResponseCode());
        paymentLogDTO.setResponseDesc(entity.getResponseDesc());
        paymentLogDTO.setResponseTimestamp(entity.getResponseTimestamp());
        paymentLogDTO.setSourceIp(entity.getSourceIp());
        paymentLogDTO.setSourceSystem(entity.getSourceSystem());
        paymentLogDTO.setSourceSystemCallBackUrl(entity.getSourceSystemCallBackUrl());
        paymentLogDTO.setFreeText1(entity.getFreeText1());
        paymentLogDTO.setFreeText2(entity.getFreeText2());
        paymentLogDTO.setHash1(entity.getHash1());
        paymentLogDTO.setHash2(entity.getHash2());
        paymentLogDTO.setRecordVersion(entity.getRecordVersion());
        paymentLogDTO.setCreatedAt(entity.getCreatedAt());
        paymentLogDTO.setUpdatedAt(entity.getUpdatedAt());
        paymentLogDTO.setSenderType(entity.getSenderType());
        paymentLogDTO.setSenderAccount(entity.getSenderAccount());
        paymentLogDTO.setSenderCurrencyCode(entity.getSenderCurrencyCode());
        paymentLogDTO.setSenderBusinessNumber(entity.getSenderBusinessNumber());
        paymentLogDTO.setSenderTranCode(entity.getSenderTranCode());
        paymentLogDTO.setSenderReference(entity.getSenderReference());
        paymentLogDTO.setSenderMsisdn(entity.getSenderMsisdn());
        paymentLogDTO.setSenderNarration(entity.getSenderNarration());
        paymentLogDTO.setSenderSenderName(entity.getSenderSenderName());
        paymentLogDTO.setSenderPartyId(entity.getSenderPartyId());
        paymentLogDTO.setSenderCountryCode(entity.getSenderCountryCode());
        paymentLogDTO.setReceiverType(entity.getReceiverType());
        paymentLogDTO.setReceiverAccount(entity.getReceiverAccount());
        paymentLogDTO.setReceiverCurrencyCode(entity.getReceiverCurrencyCode());
        paymentLogDTO.setReceiverBusinessNumber(entity.getReceiverBusinessNumber());
        paymentLogDTO.setReceiverTranCode(entity.getReceiverTranCode());
        paymentLogDTO.setReceiverReference(entity.getReceiverReference());
        paymentLogDTO.setReceiverNarration(entity.getReceiverNarration());
        paymentLogDTO.setReceiverMsisdn(entity.getReceiverMsisdn());
        paymentLogDTO.setReceiverReceiverName(entity.getReceiverReceiverName());
        paymentLogDTO.setReceiverPartyId(entity.getReceiverPartyId());
        paymentLogDTO.setReceiverCountryCode(entity.getReceiverCountryCode());
        paymentLogDTO.setCbsRequestId(entity.getCbsRequestId());
        paymentLogDTO.setCbsReq(entity.getCbsReq());
        paymentLogDTO.setCbsReqTime(entity.getCbsReqTime());
        paymentLogDTO.setCbsNarration(entity.getCbsNarration());
        paymentLogDTO.setCbsHost(entity.getCbsHost());
        paymentLogDTO.setCbsDrAcc(entity.getCbsDrAcc());
        paymentLogDTO.setCbsCrAcc(entity.getCbsCrAcc());
        paymentLogDTO.setTransactionCode(entity.getTransactionCode());
        paymentLogDTO.setCbsStatus(entity.getCbsStatus());
        paymentLogDTO.setCbsApiMode(entity.getCbsApiMode());
        paymentLogDTO.setCbsRespCode(entity.getCbsRespCode());
        paymentLogDTO.setCbsRespDesc(entity.getCbsRespDesc());
        paymentLogDTO.setCbsTranRef(entity.getCbsTranRef());
        paymentLogDTO.setCbsRespTime(entity.getCbsRespTime());
                paymentLogDTO.setCbsRetryCount(entity.getCbsRetryCount());
                paymentLogDTO.setCbsRevRespCode(entity.getCbsRevRespCode());
                paymentLogDTO.setCbsRevRespDesc(entity.getCbsRevRespDesc());
        
                paymentLogDTO.setSenderAdditionalFields(entity.getSenderAdditionalFields());
                paymentLogDTO.setReceiverAdditionalFields(entity.getReceiverAdditionalFields());
                paymentLogDTO.setDetailsAdditionalFields(entity.getDetailsAdditionalFields());
                paymentLogDTO.setCbsAdditionalFields(entity.getCbsAdditionalFields());
                paymentLogDTO.setPspAdditionalFields(entity.getPspAdditionalFields());
        
                paymentLogDTO.setPspRequestID(entity.getPspRequestID());
        
        paymentLogDTO.setPspId(entity.getPspId());
        paymentLogDTO.setPspRequestTime(entity.getPspRequestTime());
        paymentLogDTO.setPspNarration(entity.getPspNarration());
        paymentLogDTO.setPspHostURL(entity.getPspHostURL());
        paymentLogDTO.setPspDrAccount(entity.getPspDrAccount());
        paymentLogDTO.setPspCrAccount(entity.getPspCrAccount());
        paymentLogDTO.setPspStatus(entity.getPspStatus());
        paymentLogDTO.setPspApiMode(entity.getPspApiMode());
        paymentLogDTO.setPspRequest(entity.getPspRequest());
        paymentLogDTO.setPspResponse(entity.getPspResponse());
        paymentLogDTO.setPspResponseTime(entity.getPspResponseTime());
        paymentLogDTO.setPspResponseCode(entity.getPspResponseCode());
        paymentLogDTO.setPspResponseDesc(entity.getPspResponseDesc());
        paymentLogDTO.setPspResult(entity.getPspResult());
        paymentLogDTO.setPspResultCode(entity.getPspResultCode());
        paymentLogDTO.setPspResultDesc(entity.getPspResultDesc());
        paymentLogDTO.setPspReferenceCode(entity.getPspReferenceCode());
        paymentLogDTO.setPspResultTime(entity.getPspResultTime());
        paymentLogDTO.setPspRetryCount(entity.getPspRetryCount());
        paymentLogDTO.setPspRevRespCode(entity.getPspRevRespCode());
        paymentLogDTO.setPspRevRespDesc(entity.getPspRevRespDesc());

        paymentLogDTO.setStatusTimestamp(entity.getStatusTimestamp());
        paymentLogDTO.setStatusCount(entity.getStatusCount());
        paymentLogDTO.setStatusResponseCode(entity.getStatusResponseCode());
        paymentLogDTO.setStatusResponseDescription(entity.getStatusResponseDescription());

        return paymentLogDTO;
    }

    @Override
    public List<PaymentLog> toEntity(List<PaymentLogDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }

        List<PaymentLog> list = new ArrayList<>(dtoList.size());
        for (PaymentLogDTO paymentLogDTO : dtoList) {
            list.add(toEntity(paymentLogDTO));
        }

        return list;
    }

    @Override
    public List<PaymentLogDTO> toDto(List<PaymentLog> entityList) {
        if (entityList == null) {
            return null;
        }

        List<PaymentLogDTO> list = new ArrayList<>(entityList.size());
        for (PaymentLog paymentLog : entityList) {
            list.add(toDto(paymentLog));
        }

        return list;
    }
}
