package com.enterpriseflow.extraction.domain;

import com.enterpriseflow.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Purchase order data extracted from a document: the order header plus its line items.
 *
 * <p>Totals are stored exactly as stated on the document, even if they are wrong. Calculated totals
 * are derived on read so the two can be compared.
 */
@Entity
@Table(name = "order_extraction")
public class OrderExtraction extends BaseEntity {

    /** References the source document by ID only; documents belong to the document module. */
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "po_number", length = 100)
    private String poNumber;

    @Column(name = "po_date")
    private LocalDate poDate;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_email", length = 254)
    private String customerEmail;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "delivery_address", length = 500)
    private String deliveryAddress;

    @Column(name = "requested_delivery_date")
    private LocalDate requestedDeliveryDate;

    @Column(length = 3, columnDefinition = "char(3)")
    private String currency;

    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 2000)
    private String notes;

    @Column(name = "ai_confidence", precision = 3, scale = 2)
    private BigDecimal aiConfidence;

    @Column(name = "ai_provider", nullable = false, length = 50)
    private String aiProvider;

    @Column(name = "ai_model", nullable = false, length = 100)
    private String aiModel;

    /** Immutable snapshot of the validated AI output, used to tell AI-extracted values from human edits. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_raw_result", nullable = false, updatable = false)
    private Map<String, Object> aiRawResult;

    @Column(name = "extracted_at", nullable = false)
    private Instant extractedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(mappedBy = "extraction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<OrderExtractionLine> lines = new ArrayList<>();

    protected OrderExtraction() {
    }

    public OrderExtraction(UUID documentId, String aiProvider, String aiModel, BigDecimal aiConfidence,
                           Map<String, Object> aiRawResult) {
        this.documentId = documentId;
        this.aiProvider = aiProvider;
        this.aiModel = aiModel;
        this.aiConfidence = aiConfidence;
        this.aiRawResult = Map.copyOf(aiRawResult);
        this.extractedAt = Instant.now();
    }

    public OrderExtractionLine addLine(int lineNumber) {
        OrderExtractionLine line = new OrderExtractionLine(this, lineNumber);
        lines.add(line);
        return line;
    }

    public void removeLine(OrderExtractionLine line) {
        lines.remove(line);
    }

    public void markReviewed(UUID reviewer) {
        this.reviewedBy = reviewer;
        this.reviewedAt = Instant.now();
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getPoNumber() {
        return poNumber;
    }

    public void setPoNumber(String poNumber) {
        this.poNumber = poNumber;
    }

    public LocalDate getPoDate() {
        return poDate;
    }

    public void setPoDate(LocalDate poDate) {
        this.poDate = poDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public LocalDate getRequestedDeliveryDate() {
        return requestedDeliveryDate;
    }

    public void setRequestedDeliveryDate(LocalDate requestedDeliveryDate) {
        this.requestedDeliveryDate = requestedDeliveryDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public BigDecimal getAiConfidence() {
        return aiConfidence;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public String getAiModel() {
        return aiModel;
    }

    public Map<String, Object> getAiRawResult() {
        return aiRawResult;
    }

    public Instant getExtractedAt() {
        return extractedAt;
    }

    public UUID getReviewedBy() {
        return reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<OrderExtractionLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
