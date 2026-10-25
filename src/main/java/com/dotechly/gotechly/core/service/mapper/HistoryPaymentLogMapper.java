package com.dotechly.gotechly.core.service.mapper;

import com.dotechly.gotechly.core.domain.HistoryPaymentLog;
import com.dotechly.gotechly.core.service.dto.HistoryPaymentLogDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HistoryPaymentLogMapper implements EntityMapper<HistoryPaymentLogDTO, HistoryPaymentLog> {

    @Override
    public HistoryPaymentLog toEntity(HistoryPaymentLogDTO dto) {
        if (dto == null) {
            return null;
        }

        HistoryPaymentLog entity = new HistoryPaymentLog();

        entity.setId(dto.getId());
        entity.setPaymentType(dto.getPaymentType());
        entity.setRequestType(dto.getRequestType());
        entity.setRequestId(dto.getRequestId());
        entity.setRequestTimestamp(dto.getRequestTimestamp());
        entity.setAmount(dto.getAmount());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setChargeAmount(dto.getChargeAmount());
        entity.setChargeCurrencyCode(dto.getChargeCurrencyCode());
        entity.setProcessingCode(dto.getProcessingCode());
        entity.setAppInstanceId(dto.getAppInstanceId());
        entity.setAppHost(dto.getAppHost());
        entity.setStatus(dto.getStatus());
        entity.setApiMode(dto.getApiMode());
        entity.setResponseCode(dto.getResponseCode());
        entity.setResponseDesc(dto.getResponseDesc());
        entity.setResponseTimestamp(dto.getResponseTimestamp());
        entity.setSourceIp(dto.getSourceIp());
        entity.setSourceSystem(dto.getSourceSystem());
        entity.setSourceSystemCallBackUrl(dto.getSourceSystemCallBackUrl());
        entity.setFreeText1(dto.getFreeText1());
        entity.setFreeText2(dto.getFreeText2());
        entity.setHash1(dto.getHash1());
        entity.setHash2(dto.getHash2());
        entity.setRecordVersion(dto.getRecordVersion());
        entity.setCreatedAt(dto.getCreatedAt());
        entity.setUpdatedAt(dto.getUpdatedAt());
        entity.setSenderType(dto.getSenderType());
        entity.setSenderAccount(dto.getSenderAccount());
        entity.setSenderCurrencyCode(dto.getSenderCurrencyCode());
        entity.setSenderBusinessNumber(dto.getSenderBusinessNumber());
        entity.setSenderTranCode(dto.getSenderTranCode());
        entity.setSenderReference(dto.getSenderReference());
        entity.setSenderMsisdn(dto.getSenderMsisdn());
        entity.setSenderNarration(dto.getSenderNarration());
        entity.setSenderSenderName(dto.getSenderSenderName());
        entity.setSenderPartyId(dto.getSenderPartyId());
        entity.setSenderCountryCode(dto.getSenderCountryCode());
        entity.setReceiverType(dto.getReceiverType());
        entity.setReceiverAccount(dto.getReceiverAccount());
        entity.setReceiverCurrencyCode(dto.getReceiverCurrencyCode());
        entity.setReceiverBusinessNumber(dto.getReceiverBusinessNumber());
        entity.setReceiverTranCode(dto.getReceiverTranCode());
        entity.setReceiverReference(dto.getReceiverReference());
        entity.setReceiverNarration(dto.getReceiverNarration());
        entity.setReceiverMsisdn(dto.getReceiverMsisdn());
        entity.setReceiverReceiverName(dto.getReceiverReceiverName());
        entity.setReceiverPartyId(dto.getReceiverPartyId());
        entity.setReceiverCountryCode(dto.getReceiverCountryCode());
        entity.setCbsRequestId(dto.getCbsRequestId());
        entity.setCbsReq(dto.getCbsReq());
        entity.setCbsReqTime(dto.getCbsReqTime());
        entity.setCbsNarration(dto.getCbsNarration());
        entity.setCbsHost(dto.getCbsHost());
        entity.setCbsDrAcc(dto.getCbsDrAcc());
        entity.setCbsCrAcc(dto.getCbsCrAcc());
        entity.setTransactionCode(dto.getTransactionCode());
        entity.setCbsStatus(dto.getCbsStatus());
        entity.setCbsApiMode(dto.getCbsApiMode());
        entity.setCbsRespCode(dto.getCbsRespCode());
        entity.setCbsRespDesc(dto.getCbsRespDesc());
        entity.setCbsTranRef(dto.getCbsTranRef());
        entity.setCbsRespTime(dto.getCbsRespTime());
        entity.setCbsRetryCount(dto.getCbsRetryCount());
        entity.setCbsRevRespCode(dto.getCbsRevRespCode());
        entity.setCbsRevRespDesc(dto.getCbsRevRespDesc());
        entity.setSenderAdditionalFields(dto.getSenderAdditionalFields());
        entity.setReceiverAdditionalFields(dto.getReceiverAdditionalFields());
        entity.setDetailsAdditionalFields(dto.getDetailsAdditionalFields());
        entity.setCbsAdditionalFields(dto.getCbsAdditionalFields());
        entity.setPspAdditionalFields(dto.getPspAdditionalFields());
        entity.setPspRequestID(dto.getPspRequestID());
        entity.setPspId(dto.getPspId());
        entity.setPspRequestTime(dto.getPspRequestTime());
        entity.setPspNarration(dto.getPspNarration());
        entity.setPspHostURL(dto.getPspHostURL());
        entity.setPspDrAccount(dto.getPspDrAccount());
        entity.setPspCrAccount(dto.getPspCrAccount());
        entity.setPspStatus(dto.getPspStatus());
        entity.setPspApiMode(dto.getPspApiMode());
        entity.setPspRequest(dto.getPspRequest());
        entity.setPspResponse(dto.getPspResponse());
        entity.setPspResponseTime(dto.getPspResponseTime());
        entity.setPspResponseCode(dto.getPspResponseCode());
        entity.setPspResponseDesc(dto.getPspResponseDesc());
        entity.setPspResult(dto.getPspResult());
        entity.setPspResultCode(dto.getPspResultCode());
        entity.setPspResultDesc(dto.getPspResultDesc());
        entity.setPspReferenceCode(dto.getPspReferenceCode());
        entity.setPspResultTime(dto.getPspResultTime());
        entity.setPspRetryCount(dto.getPspRetryCount());
        entity.setPspRevRespCode(dto.getPspRevRespCode());
        entity.setPspRevRespDesc(dto.getPspRevRespDesc());
        entity.setStatusTimestamp(dto.getStatusTimestamp());
        entity.setStatusCount(dto.getStatusCount());
        entity.setStatusResponseCode(dto.getStatusResponseCode());
        entity.setStatusResponseDescription(dto.getStatusResponseDescription());

        return entity;
    }

    @Override
    public HistoryPaymentLogDTO toDto(HistoryPaymentLog entity) {
        if (entity == null) {
            return null;
        }

        HistoryPaymentLogDTO dto = new HistoryPaymentLogDTO();

        dto.setId(entity.getId());
        dto.setPaymentType(entity.getPaymentType());
        dto.setRequestType(entity.getRequestType());
        dto.setRequestId(entity.getRequestId());
        dto.setRequestTimestamp(entity.getRequestTimestamp());
        dto.setAmount(entity.getAmount());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setChargeAmount(entity.getChargeAmount());
        dto.setChargeCurrencyCode(entity.getChargeCurrencyCode());
        dto.setProcessingCode(entity.getProcessingCode());
        dto.setAppInstanceId(entity.getAppInstanceId());
        dto.setAppHost(entity.getAppHost());
        dto.setStatus(entity.getStatus());
        dto.setApiMode(entity.getApiMode());
        dto.setResponseCode(entity.getResponseCode());
        dto.setResponseDesc(entity.getResponseDesc());
        dto.setResponseTimestamp(entity.getResponseTimestamp());
        dto.setSourceIp(entity.getSourceIp());
        dto.setSourceSystem(entity.getSourceSystem());
        dto.setSourceSystemCallBackUrl(entity.getSourceSystemCallBackUrl());
        dto.setFreeText1(entity.getFreeText1());
        dto.setFreeText2(entity.getFreeText2());
        dto.setHash1(entity.getHash1());
        dto.setHash2(entity.getHash2());
        dto.setRecordVersion(entity.getRecordVersion());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setSenderType(entity.getSenderType());
        dto.setSenderAccount(entity.getSenderAccount());
        dto.setSenderCurrencyCode(entity.getSenderCurrencyCode());
        dto.setSenderBusinessNumber(entity.getSenderBusinessNumber());
        dto.setSenderTranCode(entity.getSenderTranCode());
        dto.setSenderReference(entity.getSenderReference());
        dto.setSenderMsisdn(entity.getSenderMsisdn());
        dto.setSenderNarration(entity.getSenderNarration());
        dto.setSenderSenderName(entity.getSenderSenderName());
        dto.setSenderPartyId(entity.getSenderPartyId());
        dto.setSenderCountryCode(entity.getSenderCountryCode());
        dto.setReceiverType(entity.getReceiverType());
        dto.setReceiverAccount(entity.getReceiverAccount());
        dto.setReceiverCurrencyCode(entity.getReceiverCurrencyCode());
        dto.setReceiverBusinessNumber(entity.getReceiverBusinessNumber());
        dto.setReceiverTranCode(entity.getReceiverTranCode());
        dto.setReceiverReference(entity.getReceiverReference());
        dto.setReceiverNarration(entity.getReceiverNarration());
        dto.setReceiverMsisdn(entity.getReceiverMsisdn());
        dto.setReceiverReceiverName(entity.getReceiverReceiverName());
        dto.setReceiverPartyId(entity.getReceiverPartyId());
        dto.setReceiverCountryCode(entity.getReceiverCountryCode());
        dto.setCbsRequestId(entity.getCbsRequestId());
        dto.setCbsReq(entity.getCbsReq());
        dto.setCbsReqTime(entity.getCbsReqTime());
        dto.setCbsNarration(entity.getCbsNarration());
        dto.setCbsHost(entity.getCbsHost());
        dto.setCbsDrAcc(entity.getCbsDrAcc());
        dto.setCbsCrAcc(entity.getCbsCrAcc());
        dto.setTransactionCode(entity.getTransactionCode());
        dto.setCbsStatus(entity.getCbsStatus());
        dto.setCbsApiMode(entity.getCbsApiMode());
        dto.setCbsRespCode(entity.getCbsRespCode());
        dto.setCbsRespDesc(entity.getCbsRespDesc());
        dto.setCbsTranRef(entity.getCbsTranRef());
        dto.setCbsRespTime(entity.getCbsRespTime());
        dto.setCbsRetryCount(entity.getCbsRetryCount());
        dto.setCbsRevRespCode(entity.getCbsRevRespCode());
        dto.setCbsRevRespDesc(entity.getCbsRevRespDesc());
        dto.setSenderAdditionalFields(entity.getSenderAdditionalFields());
        dto.setReceiverAdditionalFields(entity.getReceiverAdditionalFields());
        dto.setDetailsAdditionalFields(entity.getDetailsAdditionalFields());
        dto.setCbsAdditionalFields(entity.getCbsAdditionalFields());
        dto.setPspAdditionalFields(entity.getPspAdditionalFields());
        dto.setPspRequestID(entity.getPspRequestID());
        dto.setPspId(entity.getPspId());
        dto.setPspRequestTime(entity.getPspRequestTime());
        dto.setPspNarration(entity.getPspNarration());
        dto.setPspHostURL(entity.getPspHostURL());
        dto.setPspDrAccount(entity.getPspDrAccount());
        dto.setPspCrAccount(entity.getPspCrAccount());
        dto.setPspStatus(entity.getPspStatus());
        dto.setPspApiMode(entity.getPspApiMode());
        dto.setPspRequest(entity.getPspRequest());
        dto.setPspResponse(entity.getPspResponse());
        dto.setPspResponseTime(entity.getPspResponseTime());
        dto.setPspResponseCode(entity.getPspResponseCode());
        dto.setPspResponseDesc(entity.getPspResponseDesc());
        dto.setPspResult(entity.getPspResult());
        dto.setPspResultCode(entity.getPspResultCode());
        dto.setPspResultDesc(entity.getPspResultDesc());
        dto.setPspReferenceCode(entity.getPspReferenceCode());
        dto.setPspResultTime(entity.getPspResultTime());
        dto.setPspRetryCount(entity.getPspRetryCount());
        dto.setPspRevRespCode(entity.getPspRevRespCode());
        dto.setPspRevRespDesc(entity.getPspRevRespDesc());
        dto.setStatusTimestamp(entity.getStatusTimestamp());
        dto.setStatusCount(entity.getStatusCount());
        dto.setStatusResponseCode(entity.getStatusResponseCode());
        dto.setStatusResponseDescription(entity.getStatusResponseDescription());

        return dto;
    }

    @Override
    public List<HistoryPaymentLog> toEntity(List<HistoryPaymentLogDTO> dtoList) {
        if (dtoList == null) {
            return null;
        }
        List<HistoryPaymentLog> list = new ArrayList<>(dtoList.size());
        for (HistoryPaymentLogDTO dto : dtoList) {
            list.add(toEntity(dto));
        }
        return list;
    }

    @Override
    public List<HistoryPaymentLogDTO> toDto(List<HistoryPaymentLog> entityList) {
        if (entityList == null) {
            return null;
        }
        List<HistoryPaymentLogDTO> list = new ArrayList<>(entityList.size());
        for (HistoryPaymentLog entity : entityList) {
            list.add(toDto(entity));
        }
        return list;
    }
}
