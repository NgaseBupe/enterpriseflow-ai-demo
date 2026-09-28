package com.enterpriseflow.extraction;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Read-only view of an extracted purchase order. Totals are as stated on the document. */
public record OrderExtractionView(
        UUID id,
        UUID documentId,
        String poNumber,
        LocalDate poDate,
        String customerName,
        String customerEmail,
        String customerPhone,
        String deliveryAddress,
        LocalDate requestedDeliveryDate,
        String currency,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String notes,
        List<Line> lines,
        String aiProvider,
        String aiModel,
        BigDecimal aiConfidence,
        Instant extractedAt,
        UUID reviewedBy,
        Instant reviewedAt,
        long version) {

    public record Line(
            int lineNumber,
            String productCode,
            String description,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
    }
}
