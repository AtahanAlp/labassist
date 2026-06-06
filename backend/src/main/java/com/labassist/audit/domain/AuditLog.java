package com.labassist.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An immutable record of a security- or data-relevant action (the audit trail). */
@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(name = "at", updatable = false, nullable = false)
    private Instant at;

    /** Actor username, or null for an anonymous/unauthenticated action. */
    @Column(length = 64)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuditOutcome outcome;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    /** Free-form structured context, stored as JSON. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;
}
