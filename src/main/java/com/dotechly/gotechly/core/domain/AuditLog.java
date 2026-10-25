package com.dotechly.gotechly.core.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * An AuditLog entity.
 */
@Entity
@Table(name = "AUDIT_LOG")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator", sequenceName = "GLOBAL_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "EVENT_TYPE", length = 100, nullable = false)
    private String eventType;

    @Size(max = 100)
    @Column(name = "ENTITY_NAME", length = 100)
    private String entityName;

    @Size(max = 100)
    @Column(name = "PHASE_NAME", length = 100)
    private String phaseName;

    @Column(name = "OLD_VALUE", columnDefinition = "text")
    private String oldValue;

    @Column(name = "NEW_VALUE", columnDefinition = "text")
    private String newValue;

    @Column(name = "CHANGED_FIELDS", columnDefinition = "text")
    private String changedFields;

    @Size(max = 100)
    @Column(name = "ACTOR_ID", length = 100)
    private String actorId;

    @Size(max = 50)
    @Column(name = "ACTOR_TYPE", length = 50)
    private String actorType;

    @Size(max = 100)
    @Column(name = "ACTOR_USERNAME", length = 100)
    private String actorUsername;

    @Size(max = 100)
    @Column(name = "ROLE", length = 100)
    private String role;

    @Size(max = 50)
    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;

    @Size(max = 200)
    @Column(name = "USER_AGENT", length = 200)
    private String userAgent;

    @Size(max = 100)
    @Column(name = "SESSION_ID", length = 100)
    private String sessionId;

    @Size(max = 200)
    @Column(name = "REQUEST_ID", length = 200)
    private String requestId;

    @Size(max = 300)
    @Column(name = "ENDPOINT", length = 300)
    private String endpoint;

    @Size(max = 10)
    @Column(name = "HTTP_METHOD", length = 10)
    private String httpMethod;

    @Size(max = 20)
    @Column(name = "ENVIRONMENT", length = 20)
    private String environment;

    @Size(max = 200)
    @Column(name = "SERVICE_NAME", length = 200)
    private String serviceName;

    @Column(name = "CREATED_AT")
    private Instant createdAt;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getChangedFields() {
        return changedFields;
    }

    public void setChangedFields(String changedFields) {
        this.changedFields = changedFields;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuditLog)) {
            return false;
        }
        return id != null && id.equals(((AuditLog) o).id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AuditLog{" +
            "id=" + getId() +
            ", eventType='" + getEventType() + "'" +
            ", entityName='" + getEntityName() + "'" +
            ", phaseName='" + getPhaseName() + "'" +
            ", actorId='" + getActorId() + "'" +
            ", actorUsername='" + getActorUsername() + "'" +
            ", requestId='" + getRequestId() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
