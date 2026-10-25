package com.dotechly.gotechly.core.domain;

import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "daraja_inquiries", indexes = {
        @Index(name = "idx_drj_inq_orig_conv_id", columnList = "originator_conversation_id"),
        @Index(name = "idx_drj_inq_status", columnList = "status"),
        @Index(name = "idx_drj_inq_created_at", columnList = "created_at")
})
public class DarajaInquiry implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Status { SUBMITTED, ACKED, COMPLETED, FAILED, TIMEOUT }

    public enum CallbackStatus { PENDING, DELIVERED, FAILED, NOT_APPLICABLE }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 64)
    @Column(name = "request_id", length = 64, nullable = false, unique = true)
    private String requestId;

    @NotNull
    @Size(max = 32)
    @Column(name = "inquiry_type", length = 32, nullable = false)
    private String inquiryType;

    @Size(max = 200)
    @Column(name = "source_system", length = 200)
    private String sourceSystem;

    @Size(max = 64)
    @Column(name = "party_a", length = 64)
    private String partyA;

    @Size(max = 8)
    @Column(name = "identifier_type", length = 8)
    private String identifierType;

    @Size(max = 64)
    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Size(max = 64)
    @Column(name = "originator_conversation_id", length = 64)
    private String originatorConversationId;

    @Size(max = 16)
    @Column(name = "ack_response_code", length = 16)
    private String ackResponseCode;

    @Size(max = 255)
    @Column(name = "ack_response_description", length = 255)
    private String ackResponseDescription;

    @Size(max = 16)
    @Column(name = "result_code", length = 16)
    private String resultCode;

    @Size(max = 500)
    @Column(name = "result_description", length = 500)
    private String resultDescription;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 24, nullable = false)
    private Status status;

    @Size(max = 500)
    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "callback_status", length = 24)
    private CallbackStatus callbackStatus;

    @Column(name = "callback_attempts", nullable = false)
    private int callbackAttempts;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "request_json")
    private String requestJson;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "response_json")
    private String responseJson;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "result_json")
    private String resultJson;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ---- accessors ----------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getInquiryType() { return inquiryType; }
    public void setInquiryType(String inquiryType) { this.inquiryType = inquiryType; }

    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }

    public String getPartyA() { return partyA; }
    public void setPartyA(String partyA) { this.partyA = partyA; }

    public String getIdentifierType() { return identifierType; }
    public void setIdentifierType(String identifierType) { this.identifierType = identifierType; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getOriginatorConversationId() { return originatorConversationId; }
    public void setOriginatorConversationId(String v) { this.originatorConversationId = v; }

    public String getAckResponseCode() { return ackResponseCode; }
    public void setAckResponseCode(String v) { this.ackResponseCode = v; }

    public String getAckResponseDescription() { return ackResponseDescription; }
    public void setAckResponseDescription(String v) { this.ackResponseDescription = v; }

    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }

    public String getResultDescription() { return resultDescription; }
    public void setResultDescription(String v) { this.resultDescription = v; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    public CallbackStatus getCallbackStatus() { return callbackStatus; }
    public void setCallbackStatus(CallbackStatus callbackStatus) { this.callbackStatus = callbackStatus; }

    public int getCallbackAttempts() { return callbackAttempts; }
    public void setCallbackAttempts(int callbackAttempts) { this.callbackAttempts = callbackAttempts; }

    public String getRequestJson() { return requestJson; }
    public void setRequestJson(String requestJson) { this.requestJson = requestJson; }

    public String getResponseJson() { return responseJson; }
    public void setResponseJson(String responseJson) { this.responseJson = responseJson; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
