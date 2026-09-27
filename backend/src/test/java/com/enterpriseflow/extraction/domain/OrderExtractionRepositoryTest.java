package com.enterpriseflow.extraction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterpriseflow.RepositoryTest;
import com.enterpriseflow.TestDocuments;
import com.enterpriseflow.document.domain.Document;
import com.enterpriseflow.document.domain.DocumentRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@RepositoryTest
class OrderExtractionRepositoryTest {

    @Autowired
    OrderExtractionRepository extractions;

    @Autowired
    DocumentRepository documents;

    @Autowired
    EntityManager entityManager;

    Document document;

    @BeforeEach
    void setUp() {
        document = documents.saveAndFlush(TestDocuments.pdf());
    }

    @Test
    void savesAnExtractionWithItsLinesAndRawResult() {
        OrderExtraction extraction = newExtraction();
        extraction.setPoNumber("PO-2026-0412");
        extraction.setPoDate(LocalDate.of(2026, 9, 1));
        extraction.setCurrency("ZMW");
        extraction.setSubtotal(new BigDecimal("1250.00"));
        OrderExtractionLine second = extraction.addLine(2);
        second.setDescription("Steel bolts M8");
        second.setQuantity(new BigDecimal("100.000"));
        second.setUnitPrice(new BigDecimal("2.50"));
        second.setLineTotal(new BigDecimal("250.00"));
        OrderExtractionLine first = extraction.addLine(1);
        first.setDescription("Hex nuts M8");
        first.setQuantity(new BigDecimal("400.000"));
        first.setUnitPrice(new BigDecimal("2.50"));
        first.setLineTotal(new BigDecimal("1000.00"));
        extractions.saveAndFlush(extraction);
        entityManager.clear();

        OrderExtraction reloaded = extractions.findByDocumentId(document.getId()).orElseThrow();

        assertThat(reloaded.getPoNumber()).isEqualTo("PO-2026-0412");
        assertThat(reloaded.getSubtotal()).isEqualByComparingTo("1250.00");
        assertThat(reloaded.getLines()).extracting(OrderExtractionLine::getLineNumber).containsExactly(1, 2);
        assertThat(reloaded.getLines().get(0).getDescription()).isEqualTo("Hex nuts M8");
        assertThat(reloaded.getAiRawResult()).containsEntry("poNumber", "PO-2026-0412");
    }

    @Test
    void allowsOnlyOneExtractionPerDocument() {
        extractions.saveAndFlush(newExtraction());

        assertThatThrownBy(() -> extractions.saveAndFlush(newExtraction()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_order_extraction_document");
    }

    @Test
    void lineNumbersAreUniqueWithinAnExtraction() {
        OrderExtraction extraction = newExtraction();
        extraction.addLine(1);
        extraction.addLine(1);

        assertThatThrownBy(() -> extractions.saveAndFlush(extraction))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_order_extraction_line_number");
    }

    @Test
    void rejectsANonPositiveQuantity() {
        OrderExtraction extraction = newExtraction();
        extraction.addLine(1).setQuantity(BigDecimal.ZERO);

        assertThatThrownBy(() -> extractions.saveAndFlush(extraction))
                .hasMessageContaining("ck_order_extraction_line_quantity");
    }

    @Test
    void removingALineDeletesIt() {
        OrderExtraction extraction = newExtraction();
        extraction.addLine(1);
        extraction.addLine(2);
        extractions.saveAndFlush(extraction);
        entityManager.clear();

        OrderExtraction reloaded = extractions.findByDocumentId(document.getId()).orElseThrow();
        reloaded.removeLine(reloaded.getLines().get(1));
        extractions.saveAndFlush(reloaded);
        entityManager.clear();

        List<OrderExtractionLine> remaining = extractions.findByDocumentId(document.getId()).orElseThrow().getLines();
        assertThat(remaining).extracting(OrderExtractionLine::getLineNumber).containsExactly(1);
    }

    @Test
    void versionIncreasesOnEachUpdate() {
        OrderExtraction saved = extractions.saveAndFlush(newExtraction());
        long initialVersion = saved.getVersion();

        saved.setCustomerName("Chanda Hardware Ltd");
        OrderExtraction updated = extractions.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(initialVersion + 1);
    }

    @Test
    void keepsNullValuesInTheAiSnapshot() {
        // The AI is told to return null for anything it cannot find, so the snapshot must accept nulls.
        Map<String, Object> rawResult = new java.util.HashMap<>();
        rawResult.put("poNumber", "PO-2026-0412");
        rawResult.put("customerEmail", null);

        OrderExtraction saved = extractions.saveAndFlush(new OrderExtraction(
                document.getId(), "mock", "mock-v1", new BigDecimal("0.80"), rawResult));
        entityManager.clear();

        OrderExtraction reloaded = extractions.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getAiRawResult()).containsEntry("customerEmail", null);
    }

    private OrderExtraction newExtraction() {
        return new OrderExtraction(document.getId(), "mock", "mock-v1", new BigDecimal("0.93"),
                Map.of("poNumber", "PO-2026-0412", "lines", List.of()));
    }
}
