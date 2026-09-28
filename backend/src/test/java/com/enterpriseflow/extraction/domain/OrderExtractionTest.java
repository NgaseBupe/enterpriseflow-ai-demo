package com.enterpriseflow.extraction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Unit tests for OrderExtraction; no database involved. */
class OrderExtractionTest {

    @Test
    void acceptsNullValuesInTheAiSnapshot() {
        // Review finding R-001: the AI returns null for fields it cannot find.
        Map<String, Object> rawResult = new HashMap<>();
        rawResult.put("poNumber", "PO-2026-0412");
        rawResult.put("customerEmail", null);

        OrderExtraction extraction = newExtraction(rawResult);

        assertThat(extraction.getAiRawResult())
                .containsEntry("poNumber", "PO-2026-0412")
                .containsEntry("customerEmail", null);
    }

    @Test
    void theSnapshotCannotBeChangedAfterwards() {
        Map<String, Object> rawResult = new HashMap<>(Map.of("poNumber", "PO-2026-0412"));
        OrderExtraction extraction = newExtraction(rawResult);

        rawResult.put("poNumber", "CHANGED");

        assertThat(extraction.getAiRawResult()).containsEntry("poNumber", "PO-2026-0412");
        assertThatThrownBy(() -> extraction.getAiRawResult().put("poNumber", "CHANGED"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void addsAndRemovesLines() {
        OrderExtraction extraction = newExtraction(Map.of());
        OrderExtractionLine first = extraction.addLine(1);
        extraction.addLine(2);

        extraction.removeLine(first);

        assertThat(extraction.getLines()).extracting(OrderExtractionLine::getLineNumber).containsExactly(2);
    }

    @Test
    void linesCanOnlyBeChangedThroughTheExtraction() {
        OrderExtraction extraction = newExtraction(Map.of());

        assertThatThrownBy(() -> extraction.getLines().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void recordsWhoReviewedItAndWhen() {
        OrderExtraction extraction = newExtraction(Map.of());
        UUID reviewer = UUID.randomUUID();

        extraction.markReviewed(reviewer);

        assertThat(extraction.getReviewedBy()).isEqualTo(reviewer);
        assertThat(extraction.getReviewedAt()).isNotNull();
    }

    private static OrderExtraction newExtraction(Map<String, Object> rawResult) {
        return new OrderExtraction(UUID.randomUUID(), "mock", "mock-v1", new BigDecimal("0.90"), rawResult);
    }
}
