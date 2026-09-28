package com.enterpriseflow.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class IllegalDocumentStateExceptionTest {

    @Test
    void explainsTheConflictInPlainLanguage() {
        IllegalDocumentStateException exception =
                new IllegalDocumentStateException(DocumentStatus.EXTRACTION_FAILED, "complete extraction");

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getBody().getDetail())
                .isEqualTo("Cannot complete extraction while the document's status is \"Extraction failed\".");
        assertThat(exception.getBody().getProperties()).containsEntry("currentStatus", "EXTRACTION_FAILED");
    }

    @Test
    void everyStatusHasAReadableLabel() {
        for (DocumentStatus status : DocumentStatus.values()) {
            assertThat(status.label()).isNotBlank().doesNotContain("_");
        }
    }
}
