package com.dotechly.gotechly.core.service.criteria;

import com.dotechly.gotechly.core.service.filter.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the {@link com.dotechly.gotechly.core.domain.AuditLog} entity.
 */
public class AuditLogCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter eventType;

    private StringFilter entityName;

    private StringFilter phaseName;

    private StringFilter actorId;

    private StringFilter actorType;

    private StringFilter actorUsername;

    private StringFilter role;

    private StringFilter ipAddress;

    private StringFilter userAgent;

    private StringFilter sessionId;

    private StringFilter requestId;

    private StringFilter endpoint;

    private StringFilter httpMethod;

    private StringFilter environment;

    private StringFilter serviceName;

    private InstantFilter createdAt;

    private Boolean distinct;

    public AuditLogCriteria() {}

    public AuditLogCriteria(AuditLogCriteria other) {
        this.id = other.id == null ? null : other.id;
        this.eventType = other.eventType == null ? null : other.eventType;
        this.entityName = other.entityName == null ? null : other.entityName;
        this.phaseName = other.phaseName == null ? null : other.phaseName;
        this.actorId = other.actorId == null ? null : other.actorId;
        this.actorType = other.actorType == null ? null : other.actorType;
        this.actorUsername = other.actorUsername == null ? null : other.actorUsername;
        this.role = other.role == null ? null : other.role;
        this.ipAddress = other.ipAddress == null ? null : other.ipAddress;
        this.userAgent = other.userAgent == null ? null : other.userAgent;
        this.sessionId = other.sessionId == null ? null : other.sessionId;
        this.requestId = other.requestId == null ? null : other.requestId;
        this.endpoint = other.endpoint == null ? null : other.endpoint;
        this.httpMethod = other.httpMethod == null ? null : other.httpMethod;
        this.environment = other.environment == null ? null : other.environment;
        this.serviceName = other.serviceName == null ? null : other.serviceName;
        this.createdAt = other.createdAt == null ? null : other.createdAt;
        this.distinct = other.distinct;
    }

    public LongFilter getId() {
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getEventType() {
        return eventType;
    }

    public void setEventType(StringFilter eventType) {
        this.eventType = eventType;
    }

    public StringFilter getEntityName() {
        return entityName;
    }

    public void setEntityName(StringFilter entityName) {
        this.entityName = entityName;
    }

    public StringFilter getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(StringFilter phaseName) {
        this.phaseName = phaseName;
    }

    public StringFilter getActorId() {
        return actorId;
    }

    public void setActorId(StringFilter actorId) {
        this.actorId = actorId;
    }

    public StringFilter getActorType() {
        return actorType;
    }

    public void setActorType(StringFilter actorType) {
        this.actorType = actorType;
    }

    public StringFilter getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(StringFilter actorUsername) {
        this.actorUsername = actorUsername;
    }

    public StringFilter getRole() {
        return role;
    }

    public void setRole(StringFilter role) {
        this.role = role;
    }

    public StringFilter getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(StringFilter ipAddress) {
        this.ipAddress = ipAddress;
    }

    public StringFilter getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(StringFilter userAgent) {
        this.userAgent = userAgent;
    }

    public StringFilter getSessionId() {
        return sessionId;
    }

    public void setSessionId(StringFilter sessionId) {
        this.sessionId = sessionId;
    }

    public StringFilter getRequestId() {
        return requestId;
    }

    public void setRequestId(StringFilter requestId) {
        this.requestId = requestId;
    }

    public StringFilter getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(StringFilter endpoint) {
        this.endpoint = endpoint;
    }

    public StringFilter getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(StringFilter httpMethod) {
        this.httpMethod = httpMethod;
    }

    public StringFilter getEnvironment() {
        return environment;
    }

    public void setEnvironment(StringFilter environment) {
        this.environment = environment;
    }

    public StringFilter getServiceName() {
        return serviceName;
    }

    public void setServiceName(StringFilter serviceName) {
        this.serviceName = serviceName;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
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
        final AuditLogCriteria that = (AuditLogCriteria) o;
        return
            Objects.equals(id, that.id) &&
            Objects.equals(eventType, that.eventType) &&
            Objects.equals(entityName, that.entityName) &&
            Objects.equals(phaseName, that.phaseName) &&
            Objects.equals(actorId, that.actorId) &&
            Objects.equals(actorType, that.actorType) &&
            Objects.equals(actorUsername, that.actorUsername) &&
            Objects.equals(role, that.role) &&
            Objects.equals(ipAddress, that.ipAddress) &&
            Objects.equals(userAgent, that.userAgent) &&
            Objects.equals(sessionId, that.sessionId) &&
            Objects.equals(requestId, that.requestId) &&
            Objects.equals(endpoint, that.endpoint) &&
            Objects.equals(httpMethod, that.httpMethod) &&
            Objects.equals(environment, that.environment) &&
            Objects.equals(serviceName, that.serviceName) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(distinct, that.distinct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            eventType,
            entityName,
            phaseName,
            actorId,
            actorType,
            actorUsername,
            role,
            ipAddress,
            userAgent,
            sessionId,
            requestId,
            endpoint,
            httpMethod,
            environment,
            serviceName,
            createdAt,
            distinct
        );
    }

    @Override
    public String toString() {
        return "AuditLogCriteria{" +
            (id != null ? "id=" + id + ", " : "") +
            (eventType != null ? "eventType=" + eventType + ", " : "") +
            (entityName != null ? "entityName=" + entityName + ", " : "") +
            (phaseName != null ? "phaseName=" + phaseName + ", " : "") +
            (actorId != null ? "actorId=" + actorId + ", " : "") +
            (actorType != null ? "actorType=" + actorType + ", " : "") +
            (actorUsername != null ? "actorUsername=" + actorUsername + ", " : "") +
            (role != null ? "role=" + role + ", " : "") +
            (ipAddress != null ? "ipAddress=" + ipAddress + ", " : "") +
            (userAgent != null ? "userAgent=" + userAgent + ", " : "") +
            (sessionId != null ? "sessionId=" + sessionId + ", " : "") +
            (requestId != null ? "requestId=" + requestId + ", " : "") +
            (endpoint != null ? "endpoint=" + endpoint + ", " : "") +
            (httpMethod != null ? "httpMethod=" + httpMethod + ", " : "") +
            (environment != null ? "environment=" + environment + ", " : "") +
            (serviceName != null ? "serviceName=" + serviceName + ", " : "") +
            (createdAt != null ? "createdAt=" + createdAt + ", " : "") +
            (distinct != null ? "distinct=" + distinct + ", " : "") +
            "}";
    }
}
