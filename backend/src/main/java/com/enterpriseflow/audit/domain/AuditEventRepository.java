package com.enterpriseflow.audit.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Append-only access to the audit trail. It deliberately extends {@link Repository} rather than
 * {@code JpaRepository}, so no update or delete operations exist.
 */
public interface AuditEventRepository extends Repository<AuditEvent, UUID> {

    AuditEvent save(AuditEvent event);

    /** Events in the order they happened; the time-ordered ID breaks ties within the same microsecond. */
    List<AuditEvent> findByDocumentIdOrderByOccurredAtAscIdAsc(UUID documentId);
}
