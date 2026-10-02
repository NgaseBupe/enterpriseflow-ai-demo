package com.enterpriseflow.extraction;

import com.enterpriseflow.ai.AiDocumentExtractionService;
import com.enterpriseflow.ai.AiExtractionException;
import com.enterpriseflow.ai.DocumentInput;
import com.enterpriseflow.ai.OrderExtractionResult;
import com.enterpriseflow.audit.AuditEventType;
import com.enterpriseflow.audit.AuditService;
import com.enterpriseflow.document.DocumentContent;
import com.enterpriseflow.document.DocumentService;
import com.enterpriseflow.document.DocumentStatus;
import com.enterpriseflow.document.IllegalDocumentStateException;
import com.enterpriseflow.document.DocumentView;
import com.enterpriseflow.extraction.domain.OrderExtraction;
import com.enterpriseflow.extraction.domain.OrderExtractionLine;
import com.enterpriseflow.extraction.domain.OrderExtractionRepository;
import com.enterpriseflow.identity.CurrentUser;
import com.enterpriseflow.identity.CurrentUserProvider;
import com.enterpriseflow.identity.UserDirectory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs AI extraction for a document.
 *
 * <p>The work is split into two short transactions with the AI call in between, so a slow provider never
 * holds a database connection. If anything fails after the document is claimed, the document is marked
 * as failed with a reason the user can read, and no partial extraction is stored.
 */
@Service
public class ExtractionService {

    static final String SYSTEM_ACTOR = "SYSTEM";
    static final String UNEXPECTED_FAILURE = "The extraction could not be completed.";

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {
    };

    private final DocumentService documents;
    private final AiDocumentExtractionService ai;
    private final OrderExtractionRepository extractions;
    private final AuditService audit;
    private final CurrentUserProvider currentUserProvider;
    private final TransactionTemplate transaction;
    private final ObjectMapper objectMapper;
    private final UserDirectory userDirectory;

    ExtractionService(DocumentService documents, AiDocumentExtractionService ai, OrderExtractionRepository extractions,
                      AuditService audit, CurrentUserProvider currentUserProvider,
                      PlatformTransactionManager transactionManager, ObjectMapper objectMapper,
                      UserDirectory userDirectory) {
        this.documents = documents;
        this.ai = ai;
        this.extractions = extractions;
        this.audit = audit;
        this.currentUserProvider = currentUserProvider;
        this.transaction = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
        this.userDirectory = userDirectory;
    }

    /**
     * Extracts order data from a document that is uploaded or whose previous extraction failed.
     *
     * @return the document with its new status: EXTRACTED, or EXTRACTION_FAILED with a reason
     */
    public DocumentView extract(UUID documentId) {
        String requestedBy = currentUserProvider.currentUser().username();
        transaction.executeWithoutResult(status -> {
            documents.startProcessing(documentId);
            audit.record(documentId, AuditEventType.EXTRACTION_REQUESTED, requestedBy, null);
        });

        try {
            DocumentContent file = documents.loadContent(documentId);
            OrderExtractionResult result = ai.extract(
                    new DocumentInput(file.content(), file.contentType(), file.fileName()));
            transaction.executeWithoutResult(status -> saveResult(documentId, result));
        } catch (AiExtractionException e) {
            log.warn("AI extraction failed for document {}: {} ({})", documentId, e.reason(), e.getMessage());
            recordFailure(documentId, e.reason().userMessage(), e.reason().name());
        } catch (RuntimeException e) {
            log.error("Extraction failed unexpectedly for document {}", documentId, e);
            recordFailure(documentId, UNEXPECTED_FAILURE, "UNEXPECTED");
        }
        return documents.get(documentId);
    }

    @Transactional(readOnly = true)
    public OrderExtractionView getExtraction(UUID documentId) {
        return extractions.findByDocumentId(documentId)
                .map(this::toView)
                .orElseThrow(() -> {
                    documents.get(documentId); // 404 for an unknown document rather than a missing extraction
                    return new ExtractionNotFoundException(documentId);
                });
    }

    /**
     * Applies a reviewer's corrections. The document moves to In review, and the changed fields are
     * recorded in the audit trail. Saving without any change is accepted and records nothing.
     *
     * @throws ExtractionConflictException if the extraction changed since the reviewer loaded it, or the
     *                                     lines do not match the existing ones
     */
    @Transactional
    public OrderExtractionView updateExtraction(UUID documentId, UpdateExtractionRequest request) {
        DocumentStatus status = documents.get(documentId).status();
        if (status != DocumentStatus.EXTRACTED && status != DocumentStatus.IN_REVIEW) {
            throw new IllegalDocumentStateException(status, "edit the extracted data");
        }
        OrderExtraction extraction = extractions.findByDocumentId(documentId)
                .orElseThrow(() -> new ExtractionNotFoundException(documentId));
        if (extraction.getVersion() != request.version()) {
            throw ExtractionConflictException.staleVersion();
        }
        Map<Integer, OrderExtractionLine> linesByNumber = extraction.getLines().stream()
                .collect(Collectors.toMap(OrderExtractionLine::getLineNumber, Function.identity()));
        Map<Integer, UpdateExtractionRequest.Line> requestedLines = request.lines().stream()
                .collect(Collectors.toMap(UpdateExtractionRequest.Line::lineNumber, Function.identity(), (a, b) -> {
                    throw ExtractionConflictException.linesDoNotMatch();
                }));
        if (!requestedLines.keySet().equals(linesByNumber.keySet())) {
            throw ExtractionConflictException.linesDoNotMatch();
        }

        List<String> changes = new ArrayList<>();
        apply(changes, "poNumber", extraction.getPoNumber(), request.poNumber(), extraction::setPoNumber);
        apply(changes, "poDate", extraction.getPoDate(), request.poDate(), extraction::setPoDate);
        apply(changes, "customerName", extraction.getCustomerName(), request.customerName(), extraction::setCustomerName);
        apply(changes, "customerEmail", extraction.getCustomerEmail(), request.customerEmail(), extraction::setCustomerEmail);
        apply(changes, "customerPhone", extraction.getCustomerPhone(), request.customerPhone(), extraction::setCustomerPhone);
        apply(changes, "deliveryAddress", extraction.getDeliveryAddress(), request.deliveryAddress(),
                extraction::setDeliveryAddress);
        apply(changes, "requestedDeliveryDate", extraction.getRequestedDeliveryDate(), request.requestedDeliveryDate(),
                extraction::setRequestedDeliveryDate);
        apply(changes, "currency", extraction.getCurrency(), request.currency(), extraction::setCurrency);
        applyAmount(changes, "subtotal", extraction.getSubtotal(), request.subtotal(), extraction::setSubtotal);
        applyAmount(changes, "taxAmount", extraction.getTaxAmount(), request.taxAmount(), extraction::setTaxAmount);
        applyAmount(changes, "totalAmount", extraction.getTotalAmount(), request.totalAmount(), extraction::setTotalAmount);
        apply(changes, "notes", extraction.getNotes(), request.notes(), extraction::setNotes);
        for (OrderExtractionLine line : extraction.getLines()) {
            UpdateExtractionRequest.Line update = requestedLines.get(line.getLineNumber());
            String prefix = "lines[" + line.getLineNumber() + "].";
            apply(changes, prefix + "productCode", line.getProductCode(), update.productCode(), line::setProductCode);
            apply(changes, prefix + "description", line.getDescription(), update.description(), line::setDescription);
            applyAmount(changes, prefix + "quantity", line.getQuantity(), update.quantity(), line::setQuantity);
            apply(changes, prefix + "unitOfMeasure", line.getUnitOfMeasure(), update.unitOfMeasure(),
                    line::setUnitOfMeasure);
            applyAmount(changes, prefix + "unitPrice", line.getUnitPrice(), update.unitPrice(), line::setUnitPrice);
            applyAmount(changes, prefix + "lineTotal", line.getLineTotal(), update.lineTotal(), line::setLineTotal);
        }

        if (!changes.isEmpty()) {
            documents.markInReview(documentId);
            try {
                extractions.saveAndFlush(extraction);
            } catch (ObjectOptimisticLockingFailureException e) {
                // Another save committed between our version check and this write.
                throw ExtractionConflictException.staleVersion();
            }
            audit.record(documentId, AuditEventType.EXTRACTION_EDITED, currentUserProvider.currentUser().username(),
                    Map.of("changedFields", changes));
        }
        return toView(extraction);
    }

    /**
     * Records that a person checked the extracted data against the document and found it correct. The
     * extraction becomes read-only. This confirms the data only; it never accepts or rejects the order.
     *
     * @throws IllegalDocumentStateException unless the document is extracted or in review
     * @throws ExtractionConflictException   if the extraction changed after the reviewer loaded it
     */
    @Transactional
    public OrderExtractionView confirm(UUID documentId, ConfirmExtractionRequest request) {
        documents.markConfirmed(documentId);
        OrderExtraction extraction = extractions.findByDocumentId(documentId)
                .orElseThrow(() -> new ExtractionNotFoundException(documentId));
        if (extraction.getVersion() != request.version()) {
            throw ExtractionConflictException.staleVersion();
        }
        CurrentUser reviewer = currentUserProvider.currentUser();
        extraction.markReviewed(reviewer.id());
        try {
            extractions.saveAndFlush(extraction);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw ExtractionConflictException.staleVersion();
        }
        audit.record(documentId, AuditEventType.EXTRACTION_CONFIRMED, reviewer.username(),
                Map.of("confirmedVersion", request.version()));
        return toView(extraction);
    }

    private static <T> void apply(List<String> changes, String field, T current, T requested, Consumer<T> setter) {
        T normalised = requested instanceof String text && text.isBlank() ? null : requested;
        if (!Objects.equals(current, normalised)) {
            setter.accept(normalised);
            changes.add(field);
        }
    }

    /** Compares amounts by value, so 2.5 and 2.50 count as the same. */
    private static void applyAmount(List<String> changes, String field, BigDecimal current, BigDecimal requested,
                                    Consumer<BigDecimal> setter) {
        boolean same = current == null ? requested == null : requested != null && current.compareTo(requested) == 0;
        if (!same) {
            setter.accept(requested);
            changes.add(field);
        }
    }

    private void saveResult(UUID documentId, OrderExtractionResult result) {
        OrderExtraction extraction = new OrderExtraction(documentId, result.provider(), result.model(),
                result.confidence(), objectMapper.convertValue(result, MAP));
        extraction.setPoNumber(result.poNumber());
        extraction.setPoDate(result.poDate());
        extraction.setCustomerName(result.customerName());
        extraction.setCustomerEmail(result.customerEmail());
        extraction.setCustomerPhone(result.customerPhone());
        extraction.setDeliveryAddress(result.deliveryAddress());
        extraction.setRequestedDeliveryDate(result.requestedDeliveryDate());
        extraction.setCurrency(result.currency());
        extraction.setSubtotal(result.subtotal());
        extraction.setTaxAmount(result.taxAmount());
        extraction.setTotalAmount(result.totalAmount());
        extraction.setNotes(result.notes());
        List<OrderExtractionResult.LineItem> lines = result.lines();
        for (int i = 0; i < lines.size(); i++) {
            OrderExtractionResult.LineItem item = lines.get(i);
            OrderExtractionLine line = extraction.addLine(i + 1);
            line.setProductCode(item.productCode());
            line.setDescription(item.description());
            line.setQuantity(item.quantity());
            line.setUnitOfMeasure(item.unitOfMeasure());
            line.setUnitPrice(item.unitPrice());
            line.setLineTotal(item.lineTotal());
        }
        extractions.saveAndFlush(extraction);
        documents.markExtracted(documentId);
        audit.record(documentId, AuditEventType.EXTRACTION_SUCCEEDED, SYSTEM_ACTOR, Map.of(
                "provider", result.provider(),
                "model", result.model(),
                "lineCount", lines.size()));
    }

    private void recordFailure(UUID documentId, String userMessage, String reasonCode) {
        transaction.executeWithoutResult(status -> {
            documents.markExtractionFailed(documentId, userMessage);
            audit.record(documentId, AuditEventType.EXTRACTION_FAILED, SYSTEM_ACTOR, Map.of("reason", reasonCode));
        });
    }

    private OrderExtractionView toView(OrderExtraction e) {
        List<OrderExtractionView.Line> lines = e.getLines().stream()
                .map(l -> new OrderExtractionView.Line(l.getLineNumber(), l.getProductCode(), l.getDescription(),
                        l.getQuantity(), l.getUnitOfMeasure(), l.getUnitPrice(), l.getLineTotal()))
                .toList();
        return new OrderExtractionView(e.getId(), e.getDocumentId(), e.getPoNumber(), e.getPoDate(),
                e.getCustomerName(), e.getCustomerEmail(), e.getCustomerPhone(), e.getDeliveryAddress(),
                e.getRequestedDeliveryDate(), e.getCurrency(), e.getSubtotal(), e.getTaxAmount(), e.getTotalAmount(),
                e.getNotes(), lines, e.getAiProvider(), e.getAiModel(), e.getAiConfidence(), e.getExtractedAt(),
                e.getReviewedBy(), e.getReviewedBy() == null ? null : userDirectory.displayName(e.getReviewedBy()),
                e.getReviewedAt(), e.getVersion());
    }
}
