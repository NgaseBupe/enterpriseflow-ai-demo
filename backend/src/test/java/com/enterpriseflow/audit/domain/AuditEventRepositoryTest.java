package com.enterpriseflow.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterpriseflow.RepositoryTest;
import com.enterpriseflow.TestDocuments;
import com.enterpriseflow.audit.AuditEventType;
import com.enterpriseflow.document.domain.Document;
import com.enterpriseflow.document.domain.DocumentRepository;
import com.enterpriseflow.identity.DemoUser;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@RepositoryTest
class AuditEventRepositoryTest {

    @Autowired
    AuditEventRepository auditEvents;

    @Autowired
    DocumentRepository documents;

    @Autowired
    EntityManager entityManager;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void returnsADocumentsEventsInOrderWithTheirDetails() throws InterruptedException {
        Document document = documents.saveAndFlush(TestDocuments.pdf());
        auditEvents.save(new AuditEvent(document.getId(), AuditEventType.DOCUMENT_UPLOADED,
                DemoUser.USERNAME, Map.of("fileName", "po-1001.pdf")));
        Thread.sleep(2);
        auditEvents.save(new AuditEvent(document.getId(), AuditEventType.EXTRACTION_REQUESTED,
                DemoUser.USERNAME, java.util.Collections.singletonMap("previousValue", null)));
        entityManager.flush();
        entityManager.clear();

        List<AuditEvent> history = auditEvents.findByDocumentIdOrderByOccurredAtAscIdAsc(document.getId());

        assertThat(history).extracting(AuditEvent::getEventType)
                .containsExactly(AuditEventType.DOCUMENT_UPLOADED, AuditEventType.EXTRACTION_REQUESTED);
        assertThat(history.get(0).getDetails()).containsEntry("fileName", "po-1001.pdf");
        assertThat(history.get(1).getDetails()).containsEntry("previousValue", null);
    }

    @Test
    void breaksTimestampTiesByTheTimeOrderedId() {
        // Review finding R-004. The entity always sets "now", so identical timestamps are written with
        // plain SQL. The later ID is inserted first, so insertion order and ID order disagree.
        // Note: MySQL currently returns ties in ID order even without the explicit tie-break, because the
        // (document_id, occurred_at) index stores the primary key last. This test guards the guarantee;
        // it cannot demonstrate the failure. See docs/review-log.md, R-011.
        Document document = documents.saveAndFlush(TestDocuments.pdf());
        String earlierId = "01920000-0000-7000-8000-00000000000a";
        String laterId = "01920000-0000-7000-8000-00000000000b";
        insertEvent(laterId, document, "EXTRACTION_SUCCEEDED");
        insertEvent(earlierId, document, "EXTRACTION_REQUESTED");

        List<AuditEvent> history = auditEvents.findByDocumentIdOrderByOccurredAtAscIdAsc(document.getId());

        assertThat(history).extracting(AuditEvent::getEventType)
                .containsExactly(AuditEventType.EXTRACTION_REQUESTED, AuditEventType.EXTRACTION_SUCCEEDED);
    }

    private void insertEvent(String id, Document document, String type) {
        jdbc.update("""
                INSERT INTO audit_event (id, document_id, event_type, actor, details, occurred_at)
                VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), ?, 'demo.reviewer', NULL, '2026-09-01 10:00:00.000000')
                """, id, document.getId().toString(), type);
    }
}
