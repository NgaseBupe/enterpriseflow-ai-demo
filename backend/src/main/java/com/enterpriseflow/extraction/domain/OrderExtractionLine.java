package com.enterpriseflow.extraction.domain;

import com.enterpriseflow.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** One line item of an extracted purchase order. {@code lineTotal} is as stated on the document. */
@Entity
@Table(name = "order_extraction_line")
public class OrderExtractionLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extraction_id", nullable = false)
    private OrderExtraction extraction;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(length = 500)
    private String description;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", precision = 15, scale = 2)
    private BigDecimal lineTotal;

    protected OrderExtractionLine() {
    }

    OrderExtractionLine(OrderExtraction extraction, int lineNumber) {
        this.extraction = extraction;
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
