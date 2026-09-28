package com.enterpriseflow.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterpriseflow.audit.AuditEventType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for AuditEvent; no database involved. */
class AuditEventTest {

    @Test
    void acceptsNullValuesInItsDetails() {
        // Review finding R-001: an edit diff has a null "old value" when a field was empty before.
        Map<String, Object> details = new HashMap<>();
        details.put("field", "customerEmail");
        details.put("oldValue", null);
        details.put("newValue", "orders@chanda.example");

        AuditEvent event = new AuditEvent(UUID.randomUUID(), AuditEventType.EXTRACTION_EDITED, "reviewer", details);

        assertThat(event.getDetails()).containsEntry("oldValue", null);
    }

    @Test
    void allowsNoDetailsAtAll() {
        AuditEvent event = new AuditEvent(UUID.randomUUID(), AuditEventType.EXTRACTION_REQUESTED, "reviewer", null);

        assertThat(event.getDetails()).isNull();
    }

    @Test
    void detailsCannotBeChangedAfterwards() {
        Map<String, Object> details = new HashMap<>(Map.of("fileName", "po.pdf"));
        AuditEvent event = new AuditEvent(UUID.randomUUID(), AuditEventType.DOCUMENT_UPLOADED, "reviewer", details);

        details.put("fileName", "tampered.pdf");

        assertThat(event.getDetails()).containsEntry("fileName", "po.pdf");
        assertThatThrownBy(() -> event.getDetails().put("fileName", "tampered.pdf"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void recordsWhenItHappened() {
        AuditEvent event = new AuditEvent(UUID.randomUUID(), AuditEventType.DOCUMENT_UPLOADED, "reviewer", null);

        assertThat(event.getOccurredAt()).isNotNull();
    }
}
