package com.enterpriseflow.extraction;

import com.enterpriseflow.ai.AiDocumentExtractionService;
import com.enterpriseflow.ai.AiExtractionException;
import com.enterpriseflow.ai.DocumentInput;
import com.enterpriseflow.ai.OrderExtractionResult;
import com.enterpriseflow.audit.AuditEventType;
import com.enterpriseflow.audit.AuditService;
import com.enterpriseflow.document.DocumentContent;
import com.enterpriseflow.document.DocumentService;
import com.enterpriseflow.document.DocumentView;
import com.enterpriseflow.extraction.domain.OrderExtraction;
import com.enterpriseflow.extraction.domain.OrderExtractionLine;
import com.enterpriseflow.extraction.domain.OrderExtractionRepository;
import com.enterpriseflow.identity.CurrentUserProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    ExtractionService(DocumentService documents, AiDocumentExtractionService ai, OrderExtractionRepository extractions,
                      AuditService audit, CurrentUserProvider currentUserProvider,
                      PlatformTransactionManager transactionManager, ObjectMapper objectMapper) {
        this.documents = documents;
        this.ai = ai;
        this.extractions = extractions;
        this.audit = audit;
        this.currentUserProvider = currentUserProvider;
        this.transaction = new TransactionTemplate(transactionManager);
        this.objectMapper = objectMapper;
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
                .map(ExtractionService::toView)
                .orElseThrow(() -> {
                    documents.get(documentId); // 404 for an unknown document rather than a missing extraction
                    return new ExtractionNotFoundException(documentId);
                });
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

    private static OrderExtractionView toView(OrderExtraction e) {
        List<OrderExtractionView.Line> lines = e.getLines().stream()
                .map(l -> new OrderExtractionView.Line(l.getLineNumber(), l.getProductCode(), l.getDescription(),
                        l.getQuantity(), l.getUnitOfMeasure(), l.getUnitPrice(), l.getLineTotal()))
                .toList();
        return new OrderExtractionView(e.getId(), e.getDocumentId(), e.getPoNumber(), e.getPoDate(),
                e.getCustomerName(), e.getCustomerEmail(), e.getCustomerPhone(), e.getDeliveryAddress(),
                e.getRequestedDeliveryDate(), e.getCurrency(), e.getSubtotal(), e.getTaxAmount(), e.getTotalAmount(),
                e.getNotes(), lines, e.getAiProvider(), e.getAiModel(), e.getAiConfidence(), e.getExtractedAt(),
                e.getReviewedBy(), e.getReviewedAt(), e.getVersion());
    }
}
