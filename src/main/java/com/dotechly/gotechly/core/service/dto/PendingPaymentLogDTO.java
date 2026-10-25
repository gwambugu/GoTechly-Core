package com.dotechly.gotechly.core.service.dto;

import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public class PendingPaymentLogDTO implements Serializable {

    private Long id;

    // TRAN DETAILS
    @Size(max = 100)
    private String paymentType;

    @Size(max = 100)
    private String requestType;

    @Size(max = 50)
    private String requestId;

    private Instant requestTimestamp;

    private BigDecimal amount;

    @Size(max = 3)
    private String currencyCode;

    private BigDecimal chargeAmount;

    @Size(max = 3)
    private String chargeCurrencyCode;

    @Size(max = 20)
    private String processingCode;

    @Size(max = 100)
    private String appInstanceId;

    @Size(max = 50)
    private String appHost;

    @Size(max = 100)
    private String status;

    @Size(max = 100)
    private String apiMode;

    @Size(max = 50)
    private String responseCode;

    @Size(max = 300)
    private String responseDesc;

    private Instant responseTimestamp;

    @Size(max = 50)
    private String sourceIp;

    @Size(max = 200)
    private String sourceSystem;

    @Size(max = 300)
    private String sourceSystemCallBackUrl;

    @Size(max = 300)
    private String freeText1;

    @Size(max = 300)
    private String freeText2;

    @Size(max = 500)
    private String hash1;

    @Size(max = 500)
    private String hash2;

    @Size(max = 100)
    private String recordVersion;

    private Instant createdAt;

    private Instant updatedAt;

    // SENDER DETAILS
    @Size(max = 100)
    private String senderType;

    @Size(max = 50)
    private String senderAccount;

    @Size(max = 3)
    private String senderCurrencyCode;

    @Size(max = 50)
    private String senderBusinessNumber;

    @Size(max = 20)
    private String senderTranCode;

    @Size(max = 100)
    private String senderReference;

    @Size(max = 20)
    private String senderMsisdn;

    @Size(max = 300)
    private String senderNarration;

    @Size(max = 300)
    private String senderSenderName;

    @Size(max = 200)
    private String senderPartyId;

    @Size(max = 3)
    private String senderCountryCode;

    // RECEIVER DETAILS
    @Size(max = 100)
    private String receiverType;

    @Size(max = 50)
    private String receiverAccount;

    @Size(max = 3)
    private String receiverCurrencyCode;

    @Size(max = 50)
    private String receiverBusinessNumber;

    @Size(max = 20)
    private String receiverTranCode;

    @Size(max = 100)
    private String receiverReference;

    @Size(max = 300)
    private String receiverNarration;

    @Size(max = 20)
    private String receiverMsisdn;

    @Size(max = 300)
    private String receiverReceiverName;

    @Size(max = 200)
    private String receiverPartyId;

    @Size(max = 3)
    private String receiverCountryCode;

    // CBS DETAILS
    @Size(max = 50)
    private String cbsRequestId;

    private byte[] cbsReq;

    private Instant cbsReqTime;

    @Size(max = 300)
    private String cbsNarration;

    @Size(max = 50)
    private String cbsHost;

    @Size(max = 20)
    private String cbsDrAcc;

    @Size(max = 20)
    private String cbsCrAcc;

    @Size(max = 20)
    private String transactionCode;

    @Size(max = 100)
    private String cbsStatus;

    @Size(max = 100)
    private String cbsApiMode;

    @Size(max = 50)
    private String cbsRespCode;

    @Size(max = 300)
    private String cbsRespDesc;

    @Size(max = 50)
    private String cbsTranRef;

    private Instant cbsRespTime;

    private Integer cbsRetryCount;

    @Size(max = 50)
    private String cbsRevRespCode;

    @Size(max = 300)
    private String cbsRevRespDesc;

    private Map<String, String> senderAdditionalFields;

    private Map<String, String> receiverAdditionalFields;

    private Map<String, String> detailsAdditionalFields;

    private Map<String, String> cbsAdditionalFields;

    private Map<String, String> pspAdditionalFields;

    // PSP DETAILS
    @Size(max = 100)
    private String pspRequestID;

    @Size(max = 100)
    private String pspId;

    private Instant pspRequestTime;

    @Size(max = 300)
    private String pspNarration;

    @Size(max = 200)
    private String pspHostURL;

    @Size(max = 20)
    private String pspDrAccount;

    @Size(max = 20)
    private String pspCrAccount;

    @Size(max = 100)
    private String pspStatus;

    @Size(max = 100)
    private String pspApiMode;

    private byte[] pspRequest;

    private byte[] pspResponse;

    private Instant pspResponseTime;

    @Size(max = 100)
    private String pspResponseCode;

    @Size(max = 300)
    private String pspResponseDesc;

    private byte[] pspResult;

    @Size(max = 100)
    private String pspResultCode;

    @Size(max = 300)
    private String pspResultDesc;

    @Size(max = 100)
    private String pspReferenceCode;

    private Instant pspResultTime;

    private Integer pspRetryCount;

    @Size(max = 100)
    private String pspRevRespCode;

    @Size(max = 300)
    private String pspRevRespDesc;

    private Instant statusTimestamp;

    private Integer statusCount;

    @Size(max = 100)
    private String statusResponseCode;

    @Size(max = 300)
    private String statusResponseDescription;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Instant getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(Instant requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getChargeAmount() {
        return chargeAmount;
    }

    public void setChargeAmount(BigDecimal chargeAmount) {
        this.chargeAmount = chargeAmount;
    }

    public String getChargeCurrencyCode() {
        return chargeCurrencyCode;
    }

    public void setChargeCurrencyCode(String chargeCurrencyCode) {
        this.chargeCurrencyCode = chargeCurrencyCode;
    }

    public String getProcessingCode() {
        return processingCode;
    }

    public void setProcessingCode(String processingCode) {
        this.processingCode = processingCode;
    }

    public String getAppInstanceId() {
        return appInstanceId;
    }

    public void setAppInstanceId(String appInstanceId) {
        this.appInstanceId = appInstanceId;
    }

    public String getAppHost() {
        return appHost;
    }

    public void setAppHost(String appHost) {
        this.appHost = appHost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApiMode() {
        return apiMode;
    }

    public void setApiMode(String apiMode) {
        this.apiMode = apiMode;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseDesc() {
        return responseDesc;
    }

    public void setResponseDesc(String responseDesc) {
        this.responseDesc = responseDesc;
    }

    public Instant getResponseTimestamp() {
        return responseTimestamp;
    }

    public void setResponseTimestamp(Instant responseTimestamp) {
        this.responseTimestamp = responseTimestamp;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSourceSystemCallBackUrl() {
        return sourceSystemCallBackUrl;
    }

    public void setSourceSystemCallBackUrl(String sourceSystemCallBackUrl) {
        this.sourceSystemCallBackUrl = sourceSystemCallBackUrl;
    }

    public String getFreeText1() {
        return freeText1;
    }

    public void setFreeText1(String freeText1) {
        this.freeText1 = freeText1;
    }

    public String getFreeText2() {
        return freeText2;
    }

    public void setFreeText2(String freeText2) {
        this.freeText2 = freeText2;
    }

    public String getHash1() {
        return hash1;
    }

    public void setHash1(String hash1) {
        this.hash1 = hash1;
    }

    public String getHash2() {
        return hash2;
    }

    public void setHash2(String hash2) {
        this.hash2 = hash2;
    }

    public String getRecordVersion() {
        return recordVersion;
    }

    public void setRecordVersion(String recordVersion) {
        this.recordVersion = recordVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public String getSenderCurrencyCode() {
        return senderCurrencyCode;
    }

    public void setSenderCurrencyCode(String senderCurrencyCode) {
        this.senderCurrencyCode = senderCurrencyCode;
    }

    public String getSenderBusinessNumber() {
        return senderBusinessNumber;
    }

    public void setSenderBusinessNumber(String senderBusinessNumber) {
        this.senderBusinessNumber = senderBusinessNumber;
    }

    public String getSenderTranCode() {
        return senderTranCode;
    }

    public void setSenderTranCode(String senderTranCode) {
        this.senderTranCode = senderTranCode;
    }

    public String getSenderReference() {
        return senderReference;
    }

    public void setSenderReference(String senderReference) {
        this.senderReference = senderReference;
    }

    public String getSenderMsisdn() {
        return senderMsisdn;
    }

    public void setSenderMsisdn(String senderMsisdn) {
        this.senderMsisdn = senderMsisdn;
    }

    public String getSenderNarration() {
        return senderNarration;
    }

    public void setSenderNarration(String senderNarration) {
        this.senderNarration = senderNarration;
    }

    public String getSenderSenderName() {
        return senderSenderName;
    }

    public void setSenderSenderName(String senderSenderName) {
        this.senderSenderName = senderSenderName;
    }

    public String getSenderPartyId() {
        return senderPartyId;
    }

    public void setSenderPartyId(String senderPartyId) {
        this.senderPartyId = senderPartyId;
    }

    public String getSenderCountryCode() {
        return senderCountryCode;
    }

    public void setSenderCountryCode(String senderCountryCode) {
        this.senderCountryCode = senderCountryCode;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public String getReceiverAccount() {
        return receiverAccount;
    }

    public void setReceiverAccount(String receiverAccount) {
        this.receiverAccount = receiverAccount;
    }

    public String getReceiverCurrencyCode() {
        return receiverCurrencyCode;
    }

    public void setReceiverCurrencyCode(String receiverCurrencyCode) {
        this.receiverCurrencyCode = receiverCurrencyCode;
    }

    public String getReceiverBusinessNumber() {
        return receiverBusinessNumber;
    }

    public void setReceiverBusinessNumber(String receiverBusinessNumber) {
        this.receiverBusinessNumber = receiverBusinessNumber;
    }

    public String getReceiverTranCode() {
        return receiverTranCode;
    }

    public void setReceiverTranCode(String receiverTranCode) {
        this.receiverTranCode = receiverTranCode;
    }

    public String getReceiverReference() {
        return receiverReference;
    }

    public void setReceiverReference(String receiverReference) {
        this.receiverReference = receiverReference;
    }

    public String getReceiverNarration() {
        return receiverNarration;
    }

    public void setReceiverNarration(String receiverNarration) {
        this.receiverNarration = receiverNarration;
    }

    public String getReceiverMsisdn() {
        return receiverMsisdn;
    }

    public void setReceiverMsisdn(String receiverMsisdn) {
        this.receiverMsisdn = receiverMsisdn;
    }

    public String getReceiverReceiverName() {
        return receiverReceiverName;
    }

    public void setReceiverReceiverName(String receiverReceiverName) {
        this.receiverReceiverName = receiverReceiverName;
    }

    public String getReceiverPartyId() {
        return receiverPartyId;
    }

    public void setReceiverPartyId(String receiverPartyId) {
        this.receiverPartyId = receiverPartyId;
    }

    public String getReceiverCountryCode() {
        return receiverCountryCode;
    }

    public void setReceiverCountryCode(String receiverCountryCode) {
        this.receiverCountryCode = receiverCountryCode;
    }

    public String getCbsRequestId() {
        return cbsRequestId;
    }

    public void setCbsRequestId(String cbsRequestId) {
        this.cbsRequestId = cbsRequestId;
    }

    public byte[] getCbsReq() {
        return cbsReq;
    }

    public void setCbsReq(byte[] cbsReq) {
        this.cbsReq = cbsReq;
    }

    public Instant getCbsReqTime() {
        return cbsReqTime;
    }

    public void setCbsReqTime(Instant cbsReqTime) {
        this.cbsReqTime = cbsReqTime;
    }

    public String getCbsNarration() {
        return cbsNarration;
    }

    public void setCbsNarration(String cbsNarration) {
        this.cbsNarration = cbsNarration;
    }

    public String getCbsHost() {
        return cbsHost;
    }

    public void setCbsHost(String cbsHost) {
        this.cbsHost = cbsHost;
    }

    public String getCbsDrAcc() {
        return cbsDrAcc;
    }

    public void setCbsDrAcc(String cbsDrAcc) {
        this.cbsDrAcc = cbsDrAcc;
    }

    public String getCbsCrAcc() {
        return cbsCrAcc;
    }

    public void setCbsCrAcc(String cbsCrAcc) {
        this.cbsCrAcc = cbsCrAcc;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public String getCbsStatus() {
        return cbsStatus;
    }

    public void setCbsStatus(String cbsStatus) {
        this.cbsStatus = cbsStatus;
    }

    public String getCbsApiMode() {
        return cbsApiMode;
    }

    public void setCbsApiMode(String cbsApiMode) {
        this.cbsApiMode = cbsApiMode;
    }

    public String getCbsRespCode() {
        return cbsRespCode;
    }

    public void setCbsRespCode(String cbsRespCode) {
        this.cbsRespCode = cbsRespCode;
    }

    public String getCbsRespDesc() {
        return cbsRespDesc;
    }

    public void setCbsRespDesc(String cbsRespDesc) {
        this.cbsRespDesc = cbsRespDesc;
    }

    public String getCbsTranRef() {
        return cbsTranRef;
    }

    public void setCbsTranRef(String cbsTranRef) {
        this.cbsTranRef = cbsTranRef;
    }

    public Instant getCbsRespTime() {
        return cbsRespTime;
    }

    public void setCbsRespTime(Instant cbsRespTime) {
        this.cbsRespTime = cbsRespTime;
    }

    public Integer getCbsRetryCount() {
        return cbsRetryCount;
    }

    public void setCbsRetryCount(Integer cbsRetryCount) {
        this.cbsRetryCount = cbsRetryCount;
    }

    public String getCbsRevRespCode() {
        return cbsRevRespCode;
    }

    public void setCbsRevRespCode(String cbsRevRespCode) {
        this.cbsRevRespCode = cbsRevRespCode;
    }

    public String getCbsRevRespDesc() {
        return cbsRevRespDesc;
    }

    public void setCbsRevRespDesc(String cbsRevRespDesc) {
        this.cbsRevRespDesc = cbsRevRespDesc;
    }

    public Map<String, String> getSenderAdditionalFields() {
        return senderAdditionalFields;
    }

    public void setSenderAdditionalFields(Map<String, String> senderAdditionalFields) {
        this.senderAdditionalFields = senderAdditionalFields;
    }

    public Map<String, String> getReceiverAdditionalFields() {
        return receiverAdditionalFields;
    }

    public void setReceiverAdditionalFields(Map<String, String> receiverAdditionalFields) {
        this.receiverAdditionalFields = receiverAdditionalFields;
    }

    public Map<String, String> getDetailsAdditionalFields() {
        return detailsAdditionalFields;
    }

    public void setDetailsAdditionalFields(Map<String, String> detailsAdditionalFields) {
        this.detailsAdditionalFields = detailsAdditionalFields;
    }

    public Map<String, String> getCbsAdditionalFields() {
        return cbsAdditionalFields;
    }

    public void setCbsAdditionalFields(Map<String, String> cbsAdditionalFields) {
        this.cbsAdditionalFields = cbsAdditionalFields;
    }

    public Map<String, String> getPspAdditionalFields() {
        return pspAdditionalFields;
    }

    public void setPspAdditionalFields(Map<String, String> pspAdditionalFields) {
        this.pspAdditionalFields = pspAdditionalFields;
    }

    public String getPspRequestID() {
        return pspRequestID;
    }

    public void setPspRequestID(String pspRequestID) {
        this.pspRequestID = pspRequestID;
    }

    public String getPspId() {
        return pspId;
    }

    public void setPspId(String pspId) {
        this.pspId = pspId;
    }

    public Instant getPspRequestTime() {
        return pspRequestTime;
    }

    public void setPspRequestTime(Instant pspRequestTime) {
        this.pspRequestTime = pspRequestTime;
    }

    public String getPspNarration() {
        return pspNarration;
    }

    public void setPspNarration(String pspNarration) {
        this.pspNarration = pspNarration;
    }

    public String getPspHostURL() {
        return pspHostURL;
    }

    public void setPspHostURL(String pspHostURL) {
        this.pspHostURL = pspHostURL;
    }

    public String getPspDrAccount() {
        return pspDrAccount;
    }

    public void setPspDrAccount(String pspDrAccount) {
        this.pspDrAccount = pspDrAccount;
    }

    public String getPspCrAccount() {
        return pspCrAccount;
    }

    public void setPspCrAccount(String pspCrAccount) {
        this.pspCrAccount = pspCrAccount;
    }

    public String getPspStatus() {
        return pspStatus;
    }

    public void setPspStatus(String pspStatus) {
        this.pspStatus = pspStatus;
    }

    public String getPspApiMode() {
        return pspApiMode;
    }

    public void setPspApiMode(String pspApiMode) {
        this.pspApiMode = pspApiMode;
    }

    public byte[] getPspRequest() {
        return pspRequest;
    }

    public void setPspRequest(byte[] pspRequest) {
        this.pspRequest = pspRequest;
    }

    public byte[] getPspResponse() {
        return pspResponse;
    }

    public void setPspResponse(byte[] pspResponse) {
        this.pspResponse = pspResponse;
    }

    public Instant getPspResponseTime() {
        return pspResponseTime;
    }

    public void setPspResponseTime(Instant pspResponseTime) {
        this.pspResponseTime = pspResponseTime;
    }

    public String getPspResponseCode() {
        return pspResponseCode;
    }

    public void setPspResponseCode(String pspResponseCode) {
        this.pspResponseCode = pspResponseCode;
    }

    public String getPspResponseDesc() {
        return pspResponseDesc;
    }

    public void setPspResponseDesc(String pspResponseDesc) {
        this.pspResponseDesc = pspResponseDesc;
    }

    public byte[] getPspResult() {
        return pspResult;
    }

    public void setPspResult(byte[] pspResult) {
        this.pspResult = pspResult;
    }

    public String getPspResultCode() {
        return pspResultCode;
    }

    public void setPspResultCode(String pspResultCode) {
        this.pspResultCode = pspResultCode;
    }

    public String getPspResultDesc() {
        return pspResultDesc;
    }

    public void setPspResultDesc(String pspResultDesc) {
        this.pspResultDesc = pspResultDesc;
    }

    public String getPspReferenceCode() {
        return pspReferenceCode;
    }

    public void setPspReferenceCode(String pspReferenceCode) {
        this.pspReferenceCode = pspReferenceCode;
    }

    public Instant getPspResultTime() {
        return pspResultTime;
    }

    public void setPspResultTime(Instant pspResultTime) {
        this.pspResultTime = pspResultTime;
    }

    public Integer getPspRetryCount() {
        return pspRetryCount;
    }

    public void setPspRetryCount(Integer pspRetryCount) {
        this.pspRetryCount = pspRetryCount;
    }

    public String getPspRevRespCode() {
        return pspRevRespCode;
    }

    public void setPspRevRespCode(String pspRevRespCode) {
        this.pspRevRespCode = pspRevRespCode;
    }

    public String getPspRevRespDesc() {
        return pspRevRespDesc;
    }

    public void setPspRevRespDesc(String pspRevRespDesc) {
        this.pspRevRespDesc = pspRevRespDesc;
    }

    public Instant getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(Instant statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    public Integer getStatusCount() {
        return statusCount;
    }

    public void setStatusCount(Integer statusCount) {
        this.statusCount = statusCount;
    }

    public String getStatusResponseCode() {
        return statusResponseCode;
    }

    public void setStatusResponseCode(String statusResponseCode) {
        this.statusResponseCode = statusResponseCode;
    }

    public String getStatusResponseDescription() {
        return statusResponseDescription;
    }

    public void setStatusResponseDescription(String statusResponseDescription) {
        this.statusResponseDescription = statusResponseDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PendingPaymentLogDTO that = (PendingPaymentLogDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PendingPaymentLogDTO{" +
            "id=" + id +
            ", paymentType='" + paymentType + '\'' +
            ", requestType='" + requestType + '\'' +
            ", requestId='" + requestId + '\'' +
            ", status='" + status + '\'' +
            ", responseCode='" + responseCode + '\'' +
            ", statusResponseCode='" + statusResponseCode + '\'' +
            ", statusCount=" + statusCount +
            '}';
    }
}
