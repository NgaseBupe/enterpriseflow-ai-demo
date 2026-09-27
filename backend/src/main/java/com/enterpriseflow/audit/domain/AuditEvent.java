package com.enterpriseflow.audit.domain;

import com.enterpriseflow.audit.AuditEventType;
import com.enterpriseflow.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A single, immutable entry in a document's audit trail. */
@Entity
@Immutable
@Table(name = "audit_event")
public class AuditEvent extends BaseEntity {

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(nullable = false, length = 100)
    private String actor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(UUID documentId, AuditEventType eventType, String actor, Map<String, Object> details) {
        this.documentId = documentId;
        this.eventType = eventType;
        this.actor = actor;
        this.details = details == null ? null : Map.copyOf(details);
        this.occurredAt = Instant.now();
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
