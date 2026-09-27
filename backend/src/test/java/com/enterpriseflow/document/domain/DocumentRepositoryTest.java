package com.enterpriseflow.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterpriseflow.RepositoryTest;
import com.enterpriseflow.TestDocuments;
import com.enterpriseflow.document.DocumentStatus;
import com.enterpriseflow.identity.DemoUser;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@RepositoryTest
class DocumentRepositoryTest {

    @Autowired
    DocumentRepository documents;

    @Autowired
    EntityManager entityManager;

    @Test
    void savesAndReloadsADocument() {
        Document saved = documents.saveAndFlush(TestDocuments.pdf());
        entityManager.clear();

        Document reloaded = documents.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getId().version()).isEqualTo(7);
        assertThat(reloaded.getOriginalFileName()).isEqualTo("po-1001.pdf");
        assertThat(reloaded.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(reloaded.getUploadedBy()).isEqualTo(DemoUser.ID);
        assertThat(reloaded.isNew()).isFalse();
    }

    @Test
    void anEntityEqualsALazyProxyOfItself() {
        Document saved = documents.saveAndFlush(TestDocuments.pdf());
        entityManager.clear();
        Document proxy = entityManager.getReference(Document.class, saved.getId());
        entityManager.clear();

        Document loaded = documents.findById(saved.getId()).orElseThrow();

        assertThat(proxy.getClass()).isNotEqualTo(Document.class);
        // Only the entity's equals() is exercised: any other method called on a detached, uninitialised
        // proxy makes Hibernate try to load it, which is inherent to proxies rather than to equals().
        assertThat(loaded).isEqualTo(proxy);
    }

    @Test
    void rejectsAnEmptyFile() {
        Document empty = new Document("empty.pdf", "application/pdf", 0,
                "b".repeat(64), UUID.randomUUID().toString(), DemoUser.ID);

        // MySQL reports CHECK violations with a generic error code, so Spring doesn't classify them as
        // integrity violations. Asserting on the constraint name proves the right rule fired.
        assertThatThrownBy(() -> documents.saveAndFlush(empty))
                .hasMessageContaining("ck_document_file_size");
    }

    @Test
    void rejectsAnUnknownUploader() {
        Document orphan = new Document("po.pdf", "application/pdf", 10,
                "c".repeat(64), UUID.randomUUID().toString(), UUID.randomUUID());

        assertThatThrownBy(() -> documents.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_document_uploaded_by");
    }
}
