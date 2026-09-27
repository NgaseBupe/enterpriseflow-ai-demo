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

@RepositoryTest
class AuditEventRepositoryTest {

    @Autowired
    AuditEventRepository auditEvents;

    @Autowired
    DocumentRepository documents;

    @Autowired
    EntityManager entityManager;

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
}
