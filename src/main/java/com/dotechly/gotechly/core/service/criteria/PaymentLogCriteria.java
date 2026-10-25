package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.*;
import java.io.Serializable;
import java.util.Objects;


public class PaymentLogCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter paymentType;

    private StringFilter requestType;

    private StringFilter requestId;

    private InstantFilter requestTimestamp;

    private BigDecimalFilter amount;

    private StringFilter currencyCode;

    private BigDecimalFilter chargeAmount;

    private StringFilter chargeCurrencyCode;

    private StringFilter processingCode;

    private StringFilter appInstanceId;

    private StringFilter appHost;

    private StringFilter status;

    private StringFilter apiMode;

    private StringFilter responseCode;

    private StringFilter responseDesc;

    private InstantFilter responseTimestamp;

    private StringFilter sourceIp;

    private StringFilter sourceSystem;

    private StringFilter sourceSystemCallBackUrl;

    private StringFilter freeText1;

    private StringFilter freeText2;

    private StringFilter hash1;

    private StringFilter hash2;

    private StringFilter recordVersion;

    private InstantFilter createdAt;

    private InstantFilter updatedAt;

    private StringFilter senderType;

    private StringFilter senderAccount;

    private StringFilter senderCurrencyCode;

    private StringFilter senderBusinessNumber;

    private StringFilter senderTranCode;

    private StringFilter senderReference;

    private StringFilter senderMsisdn;

    private StringFilter senderNarration;

    private StringFilter senderSenderName;

    private StringFilter senderPartyId;

    private StringFilter senderCountryCode;

    private StringFilter receiverType;

    private StringFilter receiverAccount;

    private StringFilter receiverCurrencyCode;

    private StringFilter receiverBusinessNumber;

    private StringFilter receiverTranCode;

    private StringFilter receiverReference;

    private StringFilter receiverNarration;

    private StringFilter receiverMsisdn;

    private StringFilter receiverReceiverName;

    private StringFilter receiverPartyId;

    private StringFilter receiverCountryCode;

    private StringFilter cbsRequestId;

    private InstantFilter cbsReqTime;

    private StringFilter cbsNarration;

    private StringFilter cbsHost;

    private StringFilter cbsDrAcc;

    private StringFilter cbsCrAcc;

    private StringFilter transactionCode;

    private StringFilter cbsStatus;

    private StringFilter cbsApiMode;

    private StringFilter cbsRespCode;

    private StringFilter cbsRespDesc;

    private StringFilter cbsTranRef;

    private InstantFilter cbsRespTime;

    private IntegerFilter cbsRetryCount;

    private StringFilter cbsRevRespCode;

    private StringFilter cbsRevRespDesc;

    private StringFilter pspRequestID;

    private StringFilter pspId;

    private InstantFilter pspRequestTime;

    private StringFilter pspNarration;

    private StringFilter pspHostURL;

    private StringFilter pspDrAccount;

    private StringFilter pspCrAccount;

    private StringFilter pspStatus;

    private StringFilter pspApiMode;

    private InstantFilter pspResponseTime;

    private StringFilter pspResponseCode;

    private StringFilter pspResponseDesc;

    private StringFilter pspResultCode;

    private StringFilter pspResultDesc;

    private StringFilter pspReferenceCode;

    private InstantFilter pspResultTime;

    private IntegerFilter pspRetryCount;

    private StringFilter pspRevRespCode;

    private StringFilter pspRevRespDesc;

    private InstantFilter statusTimestamp;

    private IntegerFilter statusCount;

    private StringFilter statusResponseCode;

    private StringFilter statusResponseDescription;

    private Boolean distinct;

    public PaymentLogCriteria() {}

    public PaymentLogCriteria(PaymentLogCriteria other) {
        this.id = other.id == null ? null : other.id;
        this.paymentType = other.paymentType == null ? null : other.paymentType;
        this.requestType = other.requestType == null ? null : other.requestType;
        this.requestId = other.requestId == null ? null : other.requestId;
        this.requestTimestamp = other.requestTimestamp == null ? null : other.requestTimestamp;
        this.amount = other.amount == null ? null : other.amount;
        this.currencyCode = other.currencyCode == null ? null : other.currencyCode;
        this.chargeAmount = other.chargeAmount == null ? null : other.chargeAmount;
        this.chargeCurrencyCode = other.chargeCurrencyCode == null ? null : other.chargeCurrencyCode;
        this.processingCode = other.processingCode == null ? null : other.processingCode;
        this.appInstanceId = other.appInstanceId == null ? null : other.appInstanceId;
        this.appHost = other.appHost == null ? null : other.appHost;
        this.status = other.status == null ? null : other.status;
        this.apiMode = other.apiMode == null ? null : other.apiMode;
        this.responseCode = other.responseCode == null ? null : other.responseCode;
        this.responseDesc = other.responseDesc == null ? null : other.responseDesc;
        this.responseTimestamp = other.responseTimestamp == null ? null : other.responseTimestamp;
        this.sourceIp = other.sourceIp == null ? null : other.sourceIp;
        this.sourceSystem = other.sourceSystem == null ? null : other.sourceSystem;
        this.sourceSystemCallBackUrl = other.sourceSystemCallBackUrl == null ? null : other.sourceSystemCallBackUrl;
        this.freeText1 = other.freeText1 == null ? null : other.freeText1;
        this.freeText2 = other.freeText2 == null ? null : other.freeText2;
        this.hash1 = other.hash1 == null ? null : other.hash1;
        this.hash2 = other.hash2 == null ? null : other.hash2;
        this.recordVersion = other.recordVersion == null ? null : other.recordVersion;
        this.createdAt = other.createdAt == null ? null : other.createdAt;
        this.updatedAt = other.updatedAt == null ? null : other.updatedAt;
        this.senderType = other.senderType == null ? null : other.senderType;
        this.senderAccount = other.senderAccount == null ? null : other.senderAccount;
        this.senderCurrencyCode = other.senderCurrencyCode == null ? null : other.senderCurrencyCode;
        this.senderBusinessNumber = other.senderBusinessNumber == null ? null : other.senderBusinessNumber;
        this.senderTranCode = other.senderTranCode == null ? null : other.senderTranCode;
        this.senderReference = other.senderReference == null ? null : other.senderReference;
        this.senderMsisdn = other.senderMsisdn == null ? null : other.senderMsisdn;
        this.senderNarration = other.senderNarration == null ? null : other.senderNarration;
        this.senderSenderName = other.senderSenderName == null ? null : other.senderSenderName;
        this.senderPartyId = other.senderPartyId == null ? null : other.senderPartyId;
        this.senderCountryCode = other.senderCountryCode == null ? null : other.senderCountryCode;
        this.receiverType = other.receiverType == null ? null : other.receiverType;
        this.receiverAccount = other.receiverAccount == null ? null : other.receiverAccount;
        this.receiverCurrencyCode = other.receiverCurrencyCode == null ? null : other.receiverCurrencyCode;
        this.receiverBusinessNumber = other.receiverBusinessNumber == null ? null : other.receiverBusinessNumber;
        this.receiverTranCode = other.receiverTranCode == null ? null : other.receiverTranCode;
        this.receiverReference = other.receiverReference == null ? null : other.receiverReference;
        this.receiverNarration = other.receiverNarration == null ? null : other.receiverNarration;
        this.receiverMsisdn = other.receiverMsisdn == null ? null : other.receiverMsisdn;
        this.receiverReceiverName = other.receiverReceiverName == null ? null : other.receiverReceiverName;
        this.receiverPartyId = other.receiverPartyId == null ? null : other.receiverPartyId;
        this.receiverCountryCode = other.receiverCountryCode == null ? null : other.receiverCountryCode;
        this.cbsRequestId = other.cbsRequestId == null ? null : other.cbsRequestId;
        this.cbsReqTime = other.cbsReqTime == null ? null : other.cbsReqTime;
        this.cbsNarration = other.cbsNarration == null ? null : other.cbsNarration;
        this.cbsHost = other.cbsHost == null ? null : other.cbsHost;
        this.cbsDrAcc = other.cbsDrAcc == null ? null : other.cbsDrAcc;
        this.cbsCrAcc = other.cbsCrAcc == null ? null : other.cbsCrAcc;
        this.transactionCode = other.transactionCode == null ? null : other.transactionCode;
        this.cbsStatus = other.cbsStatus == null ? null : other.cbsStatus;
        this.cbsApiMode = other.cbsApiMode == null ? null : other.cbsApiMode;
        this.cbsRespCode = other.cbsRespCode == null ? null : other.cbsRespCode;
        this.cbsRespDesc = other.cbsRespDesc == null ? null : other.cbsRespDesc;
        this.cbsTranRef = other.cbsTranRef == null ? null : other.cbsTranRef;
        this.cbsRespTime = other.cbsRespTime == null ? null : other.cbsRespTime;
        this.cbsRetryCount = other.cbsRetryCount == null ? null : other.cbsRetryCount;
        this.cbsRevRespCode = other.cbsRevRespCode == null ? null : other.cbsRevRespCode;
        this.cbsRevRespDesc = other.cbsRevRespDesc == null ? null : other.cbsRevRespDesc;
        this.pspRequestID = other.pspRequestID == null ? null : other.pspRequestID;
        this.pspId = other.pspId == null ? null : other.pspId;
        this.pspRequestTime = other.pspRequestTime == null ? null : other.pspRequestTime;
        this.pspNarration = other.pspNarration == null ? null : other.pspNarration;
        this.pspHostURL = other.pspHostURL == null ? null : other.pspHostURL;
        this.pspDrAccount = other.pspDrAccount == null ? null : other.pspDrAccount;
        this.pspCrAccount = other.pspCrAccount == null ? null : other.pspCrAccount;
        this.pspStatus = other.pspStatus == null ? null : other.pspStatus;
        this.pspApiMode = other.pspApiMode == null ? null : other.pspApiMode;
        this.pspResponseTime = other.pspResponseTime == null ? null : other.pspResponseTime;
        this.pspResponseCode = other.pspResponseCode == null ? null : other.pspResponseCode;
        this.pspResponseDesc = other.pspResponseDesc == null ? null : other.pspResponseDesc;
        this.pspResultCode = other.pspResultCode == null ? null : other.pspResultCode;
        this.pspResultDesc = other.pspResultDesc == null ? null : other.pspResultDesc;
        this.pspReferenceCode = other.pspReferenceCode == null ? null : other.pspReferenceCode;
        this.pspResultTime = other.pspResultTime == null ? null : other.pspResultTime;
        this.pspRetryCount = other.pspRetryCount == null ? null : other.pspRetryCount;
        this.pspRevRespCode = other.pspRevRespCode == null ? null : other.pspRevRespCode;
        this.pspRevRespDesc = other.pspRevRespDesc == null ? null : other.pspRevRespDesc;
        this.statusTimestamp = other.statusTimestamp == null ? null : other.statusTimestamp;
        this.statusCount = other.statusCount == null ? null : other.statusCount;
        this.statusResponseCode = other.statusResponseCode == null ? null : other.statusResponseCode;
        this.statusResponseDescription = other.statusResponseDescription == null ? null : other.statusResponseDescription;
        this.distinct = other.distinct;
    }

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(StringFilter paymentType) {
        this.paymentType = paymentType;
    }

    public StringFilter getRequestType() {
        return requestType;
    }

    public void setRequestType(StringFilter requestType) {
        this.requestType = requestType;
    }

    public StringFilter getRequestId() {
        return requestId;
    }

    public void setRequestId(StringFilter requestId) {
        this.requestId = requestId;
    }

    public InstantFilter getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(InstantFilter requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public BigDecimalFilter getAmount() {
        return amount;
    }

    public void setAmount(BigDecimalFilter amount) {
        this.amount = amount;
    }

    public StringFilter getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(StringFilter currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimalFilter getChargeAmount() {
        return chargeAmount;
    }

    public void setChargeAmount(BigDecimalFilter chargeAmount) {
        this.chargeAmount = chargeAmount;
    }

    public StringFilter getChargeCurrencyCode() {
        return chargeCurrencyCode;
    }

    public void setChargeCurrencyCode(StringFilter chargeCurrencyCode) {
        this.chargeCurrencyCode = chargeCurrencyCode;
    }

    public StringFilter getProcessingCode() {
        return processingCode;
    }

    public void setProcessingCode(StringFilter processingCode) {
        this.processingCode = processingCode;
    }

    public StringFilter getAppInstanceId() {
        return appInstanceId;
    }

    public void setAppInstanceId(StringFilter appInstanceId) {
        this.appInstanceId = appInstanceId;
    }

    public StringFilter getAppHost() {
        return appHost;
    }

    public void setAppHost(StringFilter appHost) {
        this.appHost = appHost;
    }

    public StringFilter getStatus() {
        return status;
    }

    public void setStatus(StringFilter status) {
        this.status = status;
    }

    public StringFilter getApiMode() {
        return apiMode;
    }

    public void setApiMode(StringFilter apiMode) {
        this.apiMode = apiMode;
    }

    public StringFilter getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(StringFilter responseCode) {
        this.responseCode = responseCode;
    }

    public StringFilter getResponseDesc() {
        return responseDesc;
    }

    public void setResponseDesc(StringFilter responseDesc) {
        this.responseDesc = responseDesc;
    }

    public InstantFilter getResponseTimestamp() {
        return responseTimestamp;
    }

    public void setResponseTimestamp(InstantFilter responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }

    public StringFilter getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(StringFilter sourceIp) {
        this.sourceIp = sourceIp;
    }

    public StringFilter getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(StringFilter sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public StringFilter getSourceSystemCallBackUrl() {
        return sourceSystemCallBackUrl;
    }

    public void setSourceSystemCallBackUrl(StringFilter sourceSystemCallBackUrl) {
        this.sourceSystemCallBackUrl = sourceSystemCallBackUrl;
    }

    public StringFilter getFreeText1() {
        return freeText1;
    }

    public void setFreeText1(StringFilter freeText1) {
        this.freeText1 = freeText1;
    }

    public StringFilter getFreeText2() {
        return freeText2;
    }

    public void setFreeText2(StringFilter freeText2) {
        this.freeText2 = freeText2;
    }

    public StringFilter getHash1() {
        return hash1;
    }

    public void setHash1(StringFilter hash1) {
        this.hash1 = hash1;
    }

    public StringFilter getHash2() {
        return hash2;
    }

    public void setHash2(StringFilter hash2) {
        this.hash2 = hash2;
    }

    public StringFilter getRecordVersion() {
        return recordVersion;
    }

    public void setRecordVersion(StringFilter recordVersion) {
        this.recordVersion = recordVersion;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public InstantFilter getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(InstantFilter updatedAt) {
        this.updatedAt = updatedAt;
    }

    public StringFilter getSenderType() {
        return senderType;
    }

    public void setSenderType(StringFilter senderType) {
        this.senderType = senderType;
    }

    public StringFilter getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(StringFilter senderAccount) {
        this.senderAccount = senderAccount;
    }

    public StringFilter getSenderCurrencyCode() {
        return senderCurrencyCode;
    }

    public void setSenderCurrencyCode(StringFilter senderCurrencyCode) {
        this.senderCurrencyCode = senderCurrencyCode;
    }

    public StringFilter getSenderBusinessNumber() {
        return senderBusinessNumber;
    }

    public void setSenderBusinessNumber(StringFilter senderBusinessNumber) {
        this.senderBusinessNumber = senderBusinessNumber;
    }

    public StringFilter getSenderTranCode() {
        return senderTranCode;
    }

    public void setSenderTranCode(StringFilter senderTranCode) {
        this.senderTranCode = senderTranCode;
    }

    public StringFilter getSenderReference() {
        return senderReference;
    }

    public void setSenderReference(StringFilter senderReference) {
        this.senderReference = senderReference;
    }

    public StringFilter getSenderMsisdn() {
        return senderMsisdn;
    }

    public void setSenderMsisdn(StringFilter senderMsisdn) {
        this.senderMsisdn = senderMsisdn;
    }

    public StringFilter getSenderNarration() {
        return senderNarration;
    }

    public void setSenderNarration(StringFilter senderNarration) {
        this.senderNarration = senderNarration;
    }

    public StringFilter getSenderSenderName() {
        return senderSenderName;
    }

    public void setSenderSenderName(StringFilter senderSenderName) {
        this.senderSenderName = senderSenderName;
    }

    public StringFilter getSenderPartyId() {
        return senderPartyId;
    }

    public void setSenderPartyId(StringFilter senderPartyId) {
        this.senderPartyId = senderPartyId;
    }

    public StringFilter getSenderCountryCode() {
        return senderCountryCode;
    }

    public void setSenderCountryCode(StringFilter senderCountryCode) {
        this.senderCountryCode = senderCountryCode;
    }

    public StringFilter getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(StringFilter receiverType) {
        this.receiverType = receiverType;
    }

    public StringFilter getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(StringFilter receiverAccount) {
        this.receiverAccount = receiverAccount;
    }

    public StringFilter getReceiverCurrencyCode() {
        return receiverCurrencyCode;
    }

    public void setReceiverCurrencyCode(StringFilter receiverCurrencyCode) {
        this.receiverCurrencyCode = receiverCurrencyCode;
    }

    public StringFilter getReceiverBusinessNumber() {
        return receiverBusinessNumber;
    }

    public void setReceiverBusinessNumber(StringFilter receiverBusinessNumber) {
        this.receiverBusinessNumber = receiverBusinessNumber;
    }

    public StringFilter getReceiverTranCode() {
        return receiverTranCode;
    }

    public void setReceiverTranCode(StringFilter receiverTranCode) {
        this.receiverTranCode = receiverTranCode;
    }

    public StringFilter getReceiverReference() {
        return receiverReference;
    }

    public void setReceiverReference(StringFilter receiverReference) {
        this.receiverReference = receiverReference;
    }

    public StringFilter getReceiverNarration() {
        return receiverNarration;
    }

    public void setReceiverNarration(StringFilter receiverNarration) {
        this.receiverNarration = receiverNarration;
    }

    public StringFilter getReceiverMsisdn() {
        return receiverMsisdn;
    }

    public void setReceiverMsisdn(StringFilter receiverMsisdn) {
        this.receiverMsisdn = receiverMsisdn;
    }

    public StringFilter getReceiverReceiverName() {
        return receiverReceiverName;
    }

    public void setReceiverReceiverName(StringFilter receiverReceiverName) {
        this.receiverReceiverName = receiverReceiverName;
    }

    public StringFilter getReceiverPartyId() {
        return receiverPartyId;
    }

    public void setReceiverPartyId(StringFilter receiverPartyId) {
        this.receiverPartyId = receiverPartyId;
    }

    public StringFilter getReceiverCountryCode() {
        return receiverCountryCode;
    }

    public void setReceiverCountryCode(StringFilter receiverCountryCode) {
        this.receiverCountryCode = receiverCountryCode;
    }

    public StringFilter getCbsRequestId() {
        return cbsRequestId;
    }

    public void setCbsRequestId(StringFilter cbsRequestId) {
        this.cbsRequestId = cbsRequestId;
    }

    public InstantFilter getCbsReqTime() {
        return cbsReqTime;
    }

    public void setCbsReqTime(InstantFilter cbsReqTime) {
        this.cbsReqTime = cbsReqTime;
    }

    public StringFilter getCbsNarration() {
        return cbsNarration;
    }

    public void setCbsNarration(StringFilter cbsNarration) {
        this.cbsNarration = cbsNarration;
    }

    public StringFilter getCbsHost() {
        return cbsHost;
    }

    public void setCbsHost(StringFilter cbsHost) {
        this.cbsHost = cbsHost;
    }

    public StringFilter getCbsDrAcc() {
        return cbsDrAcc;
    }

    public void setCbsDrAcc(StringFilter cbsDrAcc) {
        this.cbsDrAcc = cbsDrAcc;
    }

    public StringFilter getCbsCrAcc() {
        return cbsCrAcc;
    }

    public void setCbsCrAcc(StringFilter cbsCrAcc) {
        this.cbsCrAcc = cbsCrAcc;
    }

    public StringFilter getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(StringFilter transactionCode) {
        this.transactionCode = transactionCode;
    }

    public StringFilter getCbsStatus() {
        return cbsStatus;
    }

    public void setCbsStatus(StringFilter cbsStatus) {
        this.cbsStatus = cbsStatus;
    }

    public StringFilter getCbsApiMode() {
        return cbsApiMode;
    }

    public void setCbsApiMode(StringFilter cbsApiMode) {
        this.cbsApiMode = cbsApiMode;
    }

    public StringFilter getCbsRespCode() {
        return cbsRespCode;
    }

    public void setCbsRespCode(StringFilter cbsRespCode) {
        this.cbsRespCode = cbsRespCode;
    }

    public StringFilter getCbsRespDesc() {
        return cbsRespDesc;
    }

    public void setCbsRespDesc(StringFilter cbsRespDesc) {
        this.cbsRespDesc = cbsRespDesc;
    }

    public StringFilter getCbsTranRef() {
        return cbsTranRef;
    }

    public void setCbsTranRef(StringFilter cbsTranRef) {
        this.cbsTranRef = cbsTranRef;
    }

    public InstantFilter getCbsRespTime() {
        return cbsRespTime;
    }

    public void setCbsRespTime(InstantFilter cbsRespTime) {
        this.cbsRespTime = cbsRespTime;
    }

    public IntegerFilter getCbsRetryCount() {
        return cbsRetryCount;
    }

    public void setCbsRetryCount(IntegerFilter cbsRetryCount) {
        this.cbsRetryCount = cbsRetryCount;
    }

    public StringFilter getCbsRevRespCode() {
        return cbsRevRespCode;
    }

    public void setCbsRevRespCode(StringFilter cbsRevRespCode) {
        this.cbsRevRespCode = cbsRevRespCode;
    }

    public StringFilter getCbsRevRespDesc() {
        return cbsRevRespDesc;
    }

    public void setCbsRevRespDesc(StringFilter cbsRevRespDesc) {
        this.cbsRevRespDesc = cbsRevRespDesc;
    }

    public StringFilter getPspRequestID() {
        return pspRequestID;
    }

    public void setPspRequestID(StringFilter pspRequestID) {
        this.pspRequestID = pspRequestID;
    }

    public StringFilter getPspId() {
        return pspId;
    }

    public void setPspId(StringFilter pspId) {
        this.pspId = pspId;
    }

    public InstantFilter getPspRequestTime() {
        return pspRequestTime;
    }

    public void setPspRequestTime(InstantFilter pspRequestTime) {
        this.pspRequestTime = pspRequestTime;
    }

    public StringFilter getPspNarration() {
        return pspNarration;
    }

    public void setPspNarration(StringFilter pspNarration) {
        this.pspNarration = pspNarration;
    }

    public StringFilter getPspHostURL() {
        return pspHostURL;
    }

    public void setPspHostURL(StringFilter pspHostURL) {
        this.pspHostURL = pspHostURL;
    }

    public StringFilter getPspDrAccount() {
        return pspDrAccount;
    }

    public void setPspDrAccount(StringFilter pspDrAccount) {
        this.pspDrAccount = pspDrAccount;
    }

    public StringFilter getPspCrAccount() {
        return pspCrAccount;
    }

    public void setPspCrAccount(StringFilter pspCrAccount) {
        this.pspCrAccount = pspCrAccount;
    }

    public StringFilter getPspStatus() {
        return pspStatus;
    }

    public void setPspStatus(StringFilter pspStatus) {
        this.pspStatus = pspStatus;
    }

    public StringFilter getPspApiMode() {
        return pspApiMode;
    }

    public void setPspApiMode(StringFilter pspApiMode) {
        this.pspApiMode = pspApiMode;
    }

    public InstantFilter getPspResponseTime() {
        return pspResponseTime;
    }

    public void setPspResponseTime(InstantFilter pspResponseTime) {
        this.pspResponseTime = pspResponseTime;
    }

    public StringFilter getPspResponseCode() {
        return pspResponseCode;
    }

    public void setPspResponseCode(StringFilter pspResponseCode) {
        this.pspResponseCode = pspResponseCode;
    }

    public StringFilter getPspResponseDesc() {
        return pspResponseDesc;
    }

    public void setPspResponseDesc(StringFilter pspResponseDesc) {
        this.pspResponseDesc = pspResponseDesc;
    }

    public StringFilter getPspResultCode() {
        return pspResultCode;
    }

    public void setPspResultCode(StringFilter pspResultCode) {
        this.pspResultCode = pspResultCode;
    }

    public StringFilter getPspResultDesc() {
        return pspResultDesc;
    }

    public void setPspResultDesc(StringFilter pspResultDesc) {
        this.pspResultDesc = pspResultDesc;
    }

    public StringFilter getPspReferenceCode() {
        return pspReferenceCode;
    }

    public void setPspReferenceCode(StringFilter pspReferenceCode) {
        this.pspReferenceCode = pspReferenceCode;
    }

    public InstantFilter getPspResultTime() {
        return pspResultTime;
    }

    public void setPspResultTime(InstantFilter pspResultTime) {
        this.pspResultTime = pspResultTime;
    }

    public IntegerFilter getPspRetryCount() {
        return pspRetryCount;
    }

    public void setPspRetryCount(IntegerFilter pspRetryCount) {
        this.pspRetryCount = pspRetryCount;
    }

    public StringFilter getPspRevRespCode() {
        return pspRevRespCode;
    }

    public void setPspRevRespCode(StringFilter pspRevRespCode) {
        this.pspRevRespCode = pspRevRespCode;
    }

    public StringFilter getPspRevRespDesc() {
        return pspRevRespDesc;
    }

    public void setPspRevRespDesc(StringFilter pspRevRespDesc) {
        this.pspRevRespDesc = pspRevRespDesc;
    }

    public InstantFilter getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(InstantFilter statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    public IntegerFilter getStatusCount() {
        return statusCount;
    }

    public void setStatusCount(IntegerFilter statusCount) {
        this.statusCount = statusCount;
    }

    public StringFilter getStatusResponseCode() {
        return statusResponseCode;
    }

    public void setStatusResponseCode(StringFilter statusResponseCode) {
        this.statusResponseCode = statusResponseCode;
    }

    public StringFilter getStatusResponseDescription() {
        return statusResponseDescription;
    }

    public void setStatusResponseDescription(StringFilter statusResponseDescription) {
        this.statusResponseDescription = statusResponseDescription;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PaymentLogCriteria that = (PaymentLogCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(paymentType, that.paymentType) &&
            Objects.equals(requestType, that.requestType) &&
            Objects.equals(requestId, that.requestId) &&
            Objects.equals(requestTimestamp, that.requestTimestamp) &&
            Objects.equals(amount, that.amount) &&
            Objects.equals(currencyCode, that.currencyCode) &&
            Objects.equals(chargeAmount, that.chargeAmount) &&
            Objects.equals(chargeCurrencyCode, that.chargeCurrencyCode) &&
            Objects.equals(processingCode, that.processingCode) &&
            Objects.equals(appInstanceId, that.appInstanceId) &&
            Objects.equals(appHost, that.appHost) &&
            Objects.equals(status, that.status) &&
            Objects.equals(apiMode, that.apiMode) &&
            Objects.equals(responseCode, that.responseCode) &&
            Objects.equals(responseDesc, that.responseDesc) &&
            Objects.equals(responseTimestamp, that.responseTimestamp) &&
            Objects.equals(sourceIp, that.sourceIp) &&
            Objects.equals(sourceSystem, that.sourceSystem) &&
            Objects.equals(sourceSystemCallBackUrl, that.sourceSystemCallBackUrl) &&
            Objects.equals(freeText1, that.freeText1) &&
            Objects.equals(freeText2, that.freeText2) &&
            Objects.equals(hash1, that.hash1) &&
            Objects.equals(hash2, that.hash2) &&
            Objects.equals(recordVersion, that.recordVersion) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(senderType, that.senderType) &&
            Objects.equals(senderAccount, that.senderAccount) &&
            Objects.equals(senderCurrencyCode, that.senderCurrencyCode) &&
            Objects.equals(senderBusinessNumber, that.senderBusinessNumber) &&
            Objects.equals(senderTranCode, that.senderTranCode) &&
            Objects.equals(senderReference, that.senderReference) &&
            Objects.equals(senderMsisdn, that.senderMsisdn) &&
            Objects.equals(senderNarration, that.senderNarration) &&
            Objects.equals(senderSenderName, that.senderSenderName) &&
            Objects.equals(senderPartyId, that.senderPartyId) &&
            Objects.equals(senderCountryCode, that.senderCountryCode) &&
            Objects.equals(receiverType, that.receiverType) &&
            Objects.equals(receiverAccount, that.receiverAccount) &&
            Objects.equals(receiverCurrencyCode, that.receiverCurrencyCode) &&
            Objects.equals(receiverBusinessNumber, that.receiverBusinessNumber) &&
            Objects.equals(receiverTranCode, that.receiverTranCode) &&
            Objects.equals(receiverReference, that.receiverReference) &&
            Objects.equals(receiverNarration, that.receiverNarration) &&
            Objects.equals(receiverMsisdn, that.receiverMsisdn) &&
            Objects.equals(receiverReceiverName, that.receiverReceiverName) &&
            Objects.equals(receiverPartyId, that.receiverPartyId) &&
            Objects.equals(receiverCountryCode, that.receiverCountryCode) &&
            Objects.equals(cbsRequestId, that.cbsRequestId) &&
            Objects.equals(cbsReqTime, that.cbsReqTime) &&
            Objects.equals(cbsNarration, that.cbsNarration) &&
            Objects.equals(cbsHost, that.cbsHost) &&
            Objects.equals(cbsDrAcc, that.cbsDrAcc) &&
            Objects.equals(cbsCrAcc, that.cbsCrAcc) &&
            Objects.equals(transactionCode, that.transactionCode) &&
            Objects.equals(cbsStatus, that.cbsStatus) &&
            Objects.equals(cbsApiMode, that.cbsApiMode) &&
            Objects.equals(cbsRespCode, that.cbsRespCode) &&
            Objects.equals(cbsRespDesc, that.cbsRespDesc) &&
            Objects.equals(cbsTranRef, that.cbsTranRef) &&
            Objects.equals(cbsRespTime, that.cbsRespTime) &&
            Objects.equals(cbsRetryCount, that.cbsRetryCount) &&
            Objects.equals(cbsRevRespCode, that.cbsRevRespCode) &&
            Objects.equals(cbsRevRespDesc, that.cbsRevRespDesc) &&
            Objects.equals(pspRequestID, that.pspRequestID) &&
            Objects.equals(pspId, that.pspId) &&
            Objects.equals(pspRequestTime, that.pspRequestTime) &&
            Objects.equals(pspNarration, that.pspNarration) &&
            Objects.equals(pspHostURL, that.pspHostURL) &&
            Objects.equals(pspDrAccount, that.pspDrAccount) &&
            Objects.equals(pspCrAccount, that.pspCrAccount) &&
            Objects.equals(pspStatus, that.pspStatus) &&
            Objects.equals(pspApiMode, that.pspApiMode) &&
            Objects.equals(pspResponseTime, that.pspResponseTime) &&
            Objects.equals(pspResponseCode, that.pspResponseCode) &&
            Objects.equals(pspResponseDesc, that.pspResponseDesc) &&
            Objects.equals(pspResultCode, that.pspResultCode) &&
            Objects.equals(pspResultDesc, that.pspResultDesc) &&
            Objects.equals(pspReferenceCode, that.pspReferenceCode) &&
            Objects.equals(pspResultTime, that.pspResultTime) &&
            Objects.equals(pspRetryCount, that.pspRetryCount) &&
            Objects.equals(pspRevRespCode, that.pspRevRespCode) &&
            Objects.equals(pspRevRespDesc, that.pspRevRespDesc) &&
            Objects.equals(statusTimestamp, that.statusTimestamp) &&
            Objects.equals(statusCount, that.statusCount) &&
            Objects.equals(statusResponseCode, that.statusResponseCode) &&
            Objects.equals(statusResponseDescription, that.statusResponseDescription) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            paymentType,
            requestType,
            requestId,
            requestTimestamp,
            amount,
            currencyCode,
            chargeAmount,
            chargeCurrencyCode,
            processingCode,
            appInstanceId,
            appHost,
            status,
            apiMode,
            responseCode,
            responseDesc,
            responseTimestamp,
            sourceIp,
            sourceSystem,
            sourceSystemCallBackUrl,
            freeText1,
            freeText2,
            hash1,
            hash2,
            recordVersion,
            createdAt,
            updatedAt,
            senderType,
            senderAccount,
            senderCurrencyCode,
            senderBusinessNumber,
            senderTranCode,
            senderReference,
            senderMsisdn,
            senderNarration,
            senderSenderName,
            senderPartyId,
            senderCountryCode,
            receiverType,
            receiverAccount,
            receiverCurrencyCode,
            receiverBusinessNumber,
            receiverTranCode,
            receiverReference,
            receiverNarration,
            receiverMsisdn,
            receiverReceiverName,
            receiverPartyId,
            receiverCountryCode,
            cbsRequestId,
            cbsReqTime,
            cbsNarration,
            cbsHost,
            cbsDrAcc,
            cbsCrAcc,
            transactionCode,
            cbsStatus,
            cbsApiMode,
            cbsRespCode,
            cbsRespDesc,
            cbsTranRef,
            cbsRespTime,
            cbsRetryCount,
            cbsRevRespCode,
            cbsRevRespDesc,
            pspRequestID,
            pspId,
            pspRequestTime,
            pspNarration,
            pspHostURL,
            pspDrAccount,
            pspCrAccount,
            pspStatus,
            pspApiMode,
            pspResponseTime,
            pspResponseCode,
            pspResponseDesc,
            pspResultCode,
            pspResultDesc,
            pspReferenceCode,
            pspResultTime,
            pspRetryCount,
            pspRevRespCode,
            pspRevRespDesc,
            statusTimestamp,
            statusCount,
            statusResponseCode,
            statusResponseDescription,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PaymentLogCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (paymentType != null ? "paymentType=" + paymentType + ", " : "") +
            (requestType != null ? "requestType=" + requestType + ", " : "") +
            (requestId != null ? "requestId=" + requestId + ", " : "") +
            (requestTimestamp != null ? "requestTimestamp=" + requestTimestamp + ", " : "") +
            (amount != null ? "amount=" + amount + ", " : "") +
            (currencyCode != null ? "currencyCode=" + currencyCode + ", " : "") +
            (chargeAmount != null ? "chargeAmount=" + chargeAmount + ", " : "") +
            (chargeCurrencyCode != null ? "chargeCurrencyCode=" + chargeCurrencyCode + ", " : "") +
            (processingCode != null ? "processingCode=" + processingCode + ", " : "") +
            (appInstanceId != null ? "appInstanceId=" + appInstanceId + ", " : "") +
            (appHost != null ? "appHost=" + appHost + ", " : "") +
            (status != null ? "status=" + status + ", " : "") +
            (apiMode != null ? "apiMode=" + apiMode + ", " : "") +
            (responseCode != null ? "responseCode=" + responseCode + ", " : "") +
            (responseDesc != null ? "responseDesc=" + responseDesc + ", " : "") +
            (responseTimestamp != null ? "responseTimestamp=" + responseTimestamp + ", " : "") +
            (sourceIp != null ? "sourceIp=" + sourceIp + ", " : "") +
            (sourceSystem != null ? "sourceSystem=" + sourceSystem + ", " : "") +
            (sourceSystemCallBackUrl != null ? "sourceSystemCallBackUrl=" + sourceSystemCallBackUrl + ", " : "") +
            (freeText1 != null ? "freeText1=" + freeText1 + ", " : "") +
            (freeText2 != null ? "freeText2=" + freeText2 + ", " : "") +
            (hash1 != null ? "hash1=" + hash1 + ", " : "") +
            (hash2 != null ? "hash2=" + hash2 + ", " : "") +
            (recordVersion != null ? "recordVersion=" + recordVersion + ", " : "") +
            (createdAt != null ? "createdAt=" + createdAt + ", " : "") +
            (updatedAt != null ? "updatedAt=" + updatedAt + ", " : "") +
            (senderType != null ? "senderType=" + senderType + ", " : "") +
            (senderAccount != null ? "senderAccount=" + senderAccount + ", " : "") +
            (senderCurrencyCode != null ? "senderCurrencyCode=" + senderCurrencyCode + ", " : "") +
            (senderBusinessNumber != null ? "senderBusinessNumber=" + senderBusinessNumber + ", " : "") +
            (senderTranCode != null ? "senderTranCode=" + senderTranCode + ", " : "") +
            (senderReference != null ? "senderReference=" + senderReference + ", " : "") +
            (senderMsisdn != null ? "senderMsisdn=" + senderMsisdn + ", " : "") +
            (senderNarration != null ? "senderNarration=" + senderNarration + ", " : "") +
            (senderSenderName != null ? "senderSenderName=" + senderSenderName + ", " : "") +
            (senderPartyId != null ? "senderPartyId=" + senderPartyId + ", " : "") +
            (senderCountryCode != null ? "senderCountryCode=" + senderCountryCode + ", " : "") +
            (receiverType != null ? "receiverType=" + receiverType + ", " : "") +
            (receiverAccount != null ? "receiverAccount=" + receiverAccount + ", " : "") +
            (receiverCurrencyCode != null ? "receiverCurrencyCode=" + receiverCurrencyCode + ", " : "") +
            (receiverBusinessNumber != null ? "receiverBusinessNumber=" + receiverBusinessNumber + ", " : "") +
            (receiverTranCode != null ? "receiverTranCode=" + receiverTranCode + ", " : "") +
            (receiverReference != null ? "receiverReference=" + receiverReference + ", " : "") +
            (receiverNarration != null ? "receiverNarration=" + receiverNarration + ", " : "") +
            (receiverMsisdn != null ? "receiverMsisdn=" + receiverMsisdn + ", " : "") +
            (receiverReceiverName != null ? "receiverReceiverName=" + receiverReceiverName + ", " : "") +
            (receiverPartyId != null ? "receiverPartyId=" + receiverPartyId + ", " : "") +
            (receiverCountryCode != null ? "receiverCountryCode=" + receiverCountryCode + ", " : "") +
            (cbsRequestId != null ? "cbsRequestId=" + cbsRequestId + ", " : "") +
            (cbsReqTime != null ? "cbsReqTime=" + cbsReqTime + ", " : "") +
            (cbsNarration != null ? "cbsNarration=" + cbsNarration + ", " : "") +
            (cbsHost != null ? "cbsHost=" + cbsHost + ", " : "") +
            (cbsDrAcc != null ? "cbsDrAcc=" + cbsDrAcc + ", " : "") +
            (cbsCrAcc != null ? "cbsCrAcc=" + cbsCrAcc + ", " : "") +
            (transactionCode != null ? "transactionCode=" + transactionCode + ", " : "") +
            (cbsStatus != null ? "cbsStatus=" + cbsStatus + ", " : "") +
            (cbsApiMode != null ? "cbsApiMode=" + cbsApiMode + ", " : "") +
            (cbsRespCode != null ? "cbsRespCode=" + cbsRespCode + ", " : "") +
            (cbsRespDesc != null ? "cbsRespDesc=" + cbsRespDesc + ", " : "") +
            (cbsTranRef != null ? "cbsTranRef=" + cbsTranRef + ", " : "") +
            (cbsRespTime != null ? "cbsRespTime=" + cbsRespTime + ", " : "") +
            (cbsRetryCount != null ? "cbsRetryCount=" + cbsRetryCount + ", " : "") +
            (cbsRevRespCode != null ? "cbsRevRespCode=" + cbsRevRespCode + ", " : "") +
            (cbsRevRespDesc != null ? "cbsRevRespDesc=" + cbsRevRespDesc + ", " : "") +
            (pspRequestID != null ? "pspRequestID=" + pspRequestID + ", " : "") +
            (pspId != null ? "pspId=" + pspId + ", " : "") +
            (pspRequestTime != null ? "pspRequestTime=" + pspRequestTime + ", " : "") +
            (pspNarration != null ? "pspNarration=" + pspNarration + ", " : "") +
            (pspHostURL != null ? "pspHostURL=" + pspHostURL + ", " : "") +
            (pspDrAccount != null ? "pspDrAccount=" + pspDrAccount + ", " : "") +
            (pspCrAccount != null ? "pspCrAccount=" + pspCrAccount + ", " : "") +
            (pspStatus != null ? "pspStatus=" + pspStatus + ", " : "") +
            (pspApiMode != null ? "pspApiMode=" + pspApiMode + ", " : "") +
            (pspResponseTime != null ? "pspResponseTime=" + pspResponseTime + ", " : "") +
            (pspResponseCode != null ? "pspResponseCode=" + pspResponseCode + ", " : "") +
            (pspResponseDesc != null ? "pspResponseDesc=" + pspResponseDesc + ", " : "") +
            (pspResultCode != null ? "pspResultCode=" + pspResultCode + ", " : "") +
            (pspResultDesc != null ? "pspResultDesc=" + pspResultDesc + ", " : "") +
            (pspReferenceCode != null ? "pspReferenceCode=" + pspReferenceCode + ", " : "") +
            (pspResultTime != null ? "pspResultTime=" + pspResultTime + ", " : "") +
            (pspRetryCount != null ? "pspRetryCount=" + pspRetryCount + ", " : "") +
            (pspRevRespCode != null ? "pspRevRespCode=" + pspRevRespCode + ", " : "") +
            (pspRevRespDesc != null ? "pspRevRespDesc=" + pspRevRespDesc + ", " : "") +
            (statusTimestamp != null ? "statusTimestamp=" + statusTimestamp + ", " : "") +
            (statusCount != null ? "statusCount=" + statusCount + ", " : "") +
            (statusResponseCode != null ? "statusResponseCode=" + statusResponseCode + ", " : "") +
            (statusResponseDescription != null ? "statusResponseDescription=" + statusResponseDescription + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
