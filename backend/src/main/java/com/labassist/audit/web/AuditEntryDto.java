package com.labassist.audit.web;

import com.labassist.audit.domain.AuditAction;
import com.labassist.audit.domain.AuditLog;
import com.labassist.audit.domain.AuditOutcome;
import java.time.Instant;
import java.util.UUID;

/** An audit-trail entry for the admin viewer. */
public record AuditEntryDto(
        UUID id,
        Instant at,
        String username,
        AuditAction action,
        AuditOutcome outcome,
        String entityType,
        String entityId,
        String details,
        String ipAddress) {

    public static AuditEntryDto from(AuditLog log) {
        return new AuditEntryDto(
                log.getId(),
                log.getAt(),
                log.getUsername(),
                log.getAction(),
                log.getOutcome(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDetails(),
                log.getIpAddress());
    }
}
