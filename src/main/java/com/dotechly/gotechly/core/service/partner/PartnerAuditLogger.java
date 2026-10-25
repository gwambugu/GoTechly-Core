package com.dotechly.gotechly.core.service.partner;

import com.dotechly.gotechly.core.domain.AuditLog;
import com.dotechly.gotechly.core.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;

/**
 * Writes AuditLog rows for partner-management actions (create / update / delete /
 * approve / reject / suspend / activate) so every change is traceable.
 *
 * <p>The {@code REQUIRES_NEW} propagation guarantees the audit row commits even if
 * the parent transaction rolls back.
 */
@Component
public class PartnerAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(PartnerAuditLogger.class);
    private static final String SERVICE_NAME = "GoTechly-Core";

    private final AuditLogRepository auditLogRepository;

    public PartnerAuditLogger(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType, String entityName, String entityId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setEventType(eventType);
            entry.setEntityName(entityName);
            entry.setActorId(currentUsername());
            entry.setActorUsername(currentUsername());
            entry.setActorType("USER");
            entry.setRequestId(entityId);
            entry.setEndpoint(currentEndpoint());
            entry.setHttpMethod(currentHttpMethod());
            entry.setIpAddress(currentIpAddress());
            entry.setNewValue(details);
            entry.setServiceName(SERVICE_NAME);
            entry.setCreatedAt(Instant.now());
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit failures must never break the business operation
            log.warn("Failed to write partner audit log: {}", e.getMessage());
        }
    }

    public static String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private static String currentEndpoint() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getRequestURI() : null;
    }

    private static String currentHttpMethod() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getMethod() : null;
    }

    private static String currentIpAddress() {
        HttpServletRequest req = currentRequest();
        return req != null ? req.getRemoteAddr() : null;
    }

    private static HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
