package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.*;
import java.io.Serializable;
import java.util.Objects;

public class PendingPaymentLogCriteria implements Serializable {

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

    public PendingPaymentLogCriteria() {}

    public PendingPaymentLogCriteria(PendingPaymentLogCriteria other) {
        this.id = other.id == null ? null : other.id;
        this.paymentType = other.paymentType == null ? null : other.paymentType;
        this.requestType = other.requestType == null ? null : other.requestType;
        this.requestId = other.requestId == null ? null : other.requestId;
        this.status = other.status == null ? null : other.status;
        this.distinct = other.distinct;
    }

    public LongFilter getId() { return id; }
    public void setId(LongFilter id) { this.id = id; }
    public StringFilter getPaymentType() { return paymentType; }
    public void setPaymentType(StringFilter paymentType) { this.paymentType = paymentType; }
    public StringFilter getRequestType() { return requestType; }
    public void setRequestType(StringFilter requestType) { this.requestType = requestType; }
    public StringFilter getRequestId() { return requestId; }
    public void setRequestId(StringFilter requestId) { this.requestId = requestId; }
    public StringFilter getStatus() { return status; }
    public void setStatus(StringFilter status) { this.status = status; }
    public Boolean getDistinct() { return distinct; }
    public void setDistinct(Boolean distinct) { this.distinct = distinct; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PendingPaymentLogCriteria that = (PendingPaymentLogCriteria) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(paymentType, that.paymentType) &&
            Objects.equals(requestType, that.requestType) &&
            Objects.equals(requestId, that.requestId) &&
            Objects.equals(status, that.status) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, paymentType, requestType, requestId, status, distinct);
    }

    @Override
    public String toString() {
        return "PendingPaymentLogCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (paymentType != null ? "paymentType=" + paymentType + ", " : "") +
            (status != null ? "status=" + status + ", " : "") +
            "}";
    }
}
