package com.enterpriseflow.extraction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A reviewer's corrections. The whole header and every line are sent, together with the version the
 * reviewer was looking at, so a save is atomic and stale edits are detected. Limits match the columns.
 */
public record UpdateExtractionRequest(
        @NotNull Long version,
        @Size(max = 100) String poNumber,
        LocalDate poDate,
        @Size(max = 200) String customerName,
        @Email @Size(max = 254) String customerEmail,
        @Size(max = 50) String customerPhone,
        @Size(max = 500) String deliveryAddress,
        LocalDate requestedDeliveryDate,
        @Pattern(regexp = "[A-Z]{3}", message = "must be a three-letter currency code, such as ZMW") String currency,
        @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal subtotal,
        @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal taxAmount,
        @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal totalAmount,
        @Size(max = 2000) String notes,
        @NotNull List<@Valid @NotNull Line> lines) {

    public record Line(
            @Positive int lineNumber,
            @Size(max = 100) String productCode,
            @Size(max = 500) String description,
            @DecimalMin(value = "0", inclusive = false, message = "must be greater than 0")
            @Digits(integer = 9, fraction = 3) BigDecimal quantity,
            @Size(max = 20) String unitOfMeasure,
            @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal unitPrice,
            @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal lineTotal) {
    }
}
