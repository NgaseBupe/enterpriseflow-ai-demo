package com.enterpriseflow.audit;

import com.enterpriseflow.audit.domain.AuditEvent;
import com.enterpriseflow.audit.domain.AuditEventRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Records audit events. Callers must already be in a transaction, so the event commits or rolls back with their change. */
@Service
public class AuditService {

    private final AuditEventRepository auditEvents;

    AuditService(AuditEventRepository auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID documentId, AuditEventType type, String actor, Map<String, Object> details) {
        auditEvents.save(new AuditEvent(documentId, type, actor, details));
    }
}
