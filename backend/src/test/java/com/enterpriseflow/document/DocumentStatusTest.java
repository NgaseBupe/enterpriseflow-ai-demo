package com.enterpriseflow.document;

import static com.enterpriseflow.document.DocumentStatus.CONFIRMED;
import static com.enterpriseflow.document.DocumentStatus.EXTRACTED;
import static com.enterpriseflow.document.DocumentStatus.EXTRACTION_FAILED;
import static com.enterpriseflow.document.DocumentStatus.IN_REVIEW;
import static com.enterpriseflow.document.DocumentStatus.PROCESSING;
import static com.enterpriseflow.document.DocumentStatus.UPLOADED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Checks every possible pair of states, so no transition is allowed or forbidden by accident. */
class DocumentStatusTest {

    private static final Map<DocumentStatus, Set<DocumentStatus>> ALLOWED = Map.of(
            UPLOADED, EnumSet.of(PROCESSING),
            PROCESSING, EnumSet.of(EXTRACTED, EXTRACTION_FAILED),
            EXTRACTION_FAILED, EnumSet.of(PROCESSING),
            EXTRACTED, EnumSet.of(IN_REVIEW, CONFIRMED),
            IN_REVIEW, EnumSet.of(IN_REVIEW, CONFIRMED),
            CONFIRMED, EnumSet.noneOf(DocumentStatus.class));

    @Test
    void allowsExactlyTheDocumentedTransitions() {
        for (DocumentStatus from : DocumentStatus.values()) {
            for (DocumentStatus to : DocumentStatus.values()) {
                assertThat(from.canTransitionTo(to))
                        .as("%s -> %s", from, to)
                        .isEqualTo(ALLOWED.get(from).contains(to));
            }
        }
    }

    @Test
    void extractionCanStartFromUploadedOrAfterAFailure() {
        assertThat(DocumentStatus.EXTRACTABLE).containsExactlyInAnyOrder(UPLOADED, EXTRACTION_FAILED);
    }
}
