package com.enterpriseflow.ai.provider.mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterpriseflow.TestFiles;
import com.enterpriseflow.ai.AiExtractionException;
import com.enterpriseflow.ai.DocumentInput;
import com.enterpriseflow.ai.OrderExtractionResult;
import com.enterpriseflow.ai.OrderExtractionResult.LineItem;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MockExtractionProviderTest {

    private final MockExtractionProvider provider = new MockExtractionProvider();

    @Test
    void returnsTheSameResultForTheSameFile() {
        DocumentInput input = pdf("po-1001.pdf");

        assertThat(provider.extract(input)).isEqualTo(provider.extract(input));
    }

    @Test
    void takesThePoNumberFromTheFileName() {
        assertThat(provider.extract(pdf("po-1001.pdf")).poNumber()).isEqualTo("PO-1001");
        assertThat(provider.extract(pdf("scan.pdf")).poNumber()).isEqualTo("PO-0001");
    }

    @Test
    void returnsAnInternallyConsistentOrder() {
        OrderExtractionResult result = provider.extract(pdf("po-1001.pdf"));

        assertThat(result.lines()).hasSize(3).allSatisfy(line ->
                assertThat(line.lineTotal()).isEqualByComparingTo(line.quantity().multiply(line.unitPrice())));
        BigDecimal sumOfLines = result.lines().stream().map(LineItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(result.subtotal()).isEqualByComparingTo(sumOfLines);
        assertThat(result.totalAmount()).isEqualByComparingTo(result.subtotal().add(result.taxAmount()));
        assertThat(result.currency()).isEqualTo("ZMW");
    }

    @Test
    void identifiesItselfAsTheProvider() {
        OrderExtractionResult result = provider.extract(pdf("po-1001.pdf"));

        assertThat(result.provider()).isEqualTo("mock");
        assertThat(result.model()).isEqualTo("mock-v1");
        assertThat(result.confidence()).isBetween(BigDecimal.ZERO, BigDecimal.ONE);
    }

    @Test
    void simulatesAProviderFailureForFilesNamedFail() {
        assertThatThrownBy(() -> provider.extract(pdf("po-FAIL-1.pdf")))
                .isInstanceOfSatisfying(AiExtractionException.class,
                        e -> assertThat(e.reason()).isEqualTo(AiExtractionException.Reason.PROVIDER_ERROR));
    }

    private static DocumentInput pdf(String name) {
        return new DocumentInput(TestFiles.PDF, "application/pdf", name);
    }
}
