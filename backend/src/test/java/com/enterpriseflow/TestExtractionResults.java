package com.enterpriseflow;

import com.enterpriseflow.ai.OrderExtractionResult;
import com.enterpriseflow.ai.OrderExtractionResult.LineItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Synthetic AI results for tests that replace the AI provider. */
public final class TestExtractionResults {

    private TestExtractionResults() {
    }

    public static OrderExtractionResult valid() {
        return withLines(List.of(new LineItem("SKU-1", "Cable ties 300 mm", new BigDecimal("4"), "pack",
                new BigDecimal("12.50"), new BigDecimal("50.00"))));
    }

    public static OrderExtractionResult withLines(List<LineItem> lines) {
        return new OrderExtractionResult("PO-7001", LocalDate.of(2026, 9, 2), "Test Customer Ltd", null, null,
                null, null, "ZMW", new BigDecimal("50.00"), new BigDecimal("8.00"), new BigDecimal("58.00"), null,
                lines, new BigDecimal("0.88"), "test-provider", "test-model");
    }
}
