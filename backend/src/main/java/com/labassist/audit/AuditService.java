package com.labassist.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labassist.audit.domain.AuditAction;
import com.labassist.audit.domain.AuditLog;
import com.labassist.audit.domain.AuditOutcome;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes entries to the {@code audit_log} table — the application's "logging
 * system" for security- and data-relevant actions (logins, polls, views, LLM use).
 *
 * <p>Records run in their own transaction so an audit write never rolls back, and
 * is never rolled back by, the surrounding business transaction.
 */
@Slf4j
@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, AuditOutcome outcome, String username,
                       String entityType, String entityId, Map<String, Object> details, String ipAddress) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setOutcome(outcome);
            entry.setUsername(username);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId);
            entry.setIpAddress(ipAddress);
            entry.setDetails(toJson(details));
            repository.save(entry);
        } catch (Exception e) {
            // Auditing must never break the primary flow; log and move on.
            log.warn("Failed to write audit entry action={} outcome={}", action, outcome, e);
        }
    }

    public void success(AuditAction action, String username, String entityType, String entityId,
                        Map<String, Object> details, String ipAddress) {
        record(action, AuditOutcome.SUCCESS, username, entityType, entityId, details, ipAddress);
    }

    public void failure(AuditAction action, String username, String entityType, String entityId,
                        Map<String, Object> details, String ipAddress) {
        record(action, AuditOutcome.FAILURE, username, entityType, entityId, details, ipAddress);
    }

    private String toJson(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            return "{\"_serializationError\":true}";
        }
    }
}
