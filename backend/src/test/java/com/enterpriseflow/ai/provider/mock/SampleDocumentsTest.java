package com.enterpriseflow.ai.provider.mock;

import static org.assertj.core.api.Assertions.assertThat;

import com.enterpriseflow.ai.DocumentInput;
import com.enterpriseflow.ai.OrderExtractionResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The synthetic samples in /sample-documents are printed with exactly what the mock provider returns
 * for them, so a demo with the mock shows matching data (review finding R-025). If the mock's data
 * or a sample changes, this test fails instead of the demo silently showing mismatched values.
 */
class SampleDocumentsTest {

    private static final Path SAMPLES = Path.of("..", "sample-documents");

    private final MockExtractionProvider provider = new MockExtractionProvider();

    @ParameterizedTest
    @CsvSource({
            "po-1001.pdf,      application/pdf, PO-1001, Chanda Hardware Ltd",
            "po-1002.pdf,      application/pdf, PO-1002, Mwila Building Supplies",
            "po-1003-scan.png, image/png,       PO-1003, Kasonde Engineering",
    })
    void theMockReturnsWhatTheSampleShows(String file, String mediaType, String poNumber, String customer)
            throws IOException {
        byte[] content = Files.readAllBytes(SAMPLES.resolve(file));

        OrderExtractionResult result = provider.extract(new DocumentInput(content, mediaType, file));

        assertThat(result.poNumber()).isEqualTo(poNumber);
        assertThat(result.customerName()).isEqualTo(customer);
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("2209.80"));
    }
}
