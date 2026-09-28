package com.enterpriseflow.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Provider-neutral purchase order data as read by an AI model. Any field may be null when the model
 * could not find it. Totals are as printed on the document; they are never calculated by the model.
 */
public record OrderExtractionResult(
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
        List<LineItem> lines,
        BigDecimal confidence,
        String provider,
        String model) {

    public OrderExtractionResult {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public record LineItem(
            String productCode,
            String description,
            BigDecimal quantity,
            String unitOfMeasure,
            BigDecimal unitPrice,
            BigDecimal lineTotal) {
    }
}
