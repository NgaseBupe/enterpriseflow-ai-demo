package com.enterpriseflow.extraction.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterpriseflow.TestExtractionResults;
import com.enterpriseflow.TestFiles;
import com.enterpriseflow.TestcontainersConfiguration;
import com.enterpriseflow.ai.AiDocumentExtractionService;
import com.enterpriseflow.ai.AiExtractionException;
import com.enterpriseflow.ai.OrderExtractionResult.LineItem;
import com.enterpriseflow.document.DocumentService;
import com.enterpriseflow.document.IllegalDocumentStateException;
import com.enterpriseflow.extraction.domain.OrderExtractionRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Failure, retry and concurrency behaviour, with the AI provider replaced by a Mockito mock. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExtractionFailureIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OrderExtractionRepository extractions;

    @Autowired
    DocumentService documentService;

    @MockitoBean
    AiDocumentExtractionService ai;

    @Test
    void aFailedExtractionCanBeRetried() throws Exception {
        when(ai.extract(any()))
                .thenThrow(new AiExtractionException(AiExtractionException.Reason.TIMEOUT, "timed out"))
                .thenReturn(TestExtractionResults.valid());
        UUID id = upload("po-2001.pdf");

        mockMvc.perform(post("/api/documents/{id}/process", id))
                .andExpect(jsonPath("$.status").value("EXTRACTION_FAILED"))
                .andExpect(jsonPath("$.failureReason").value("The AI service took too long to respond."));

        mockMvc.perform(post("/api/documents/{id}/process", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXTRACTED"))
                .andExpect(jsonPath("$.failureReason").doesNotExist());
        assertThat(extractions.findByDocumentId(id)).isPresent();
    }

    @Test
    void dataTheDatabaseRejectsLeavesNoPartialExtraction() throws Exception {
        // The second line has a zero quantity, which the database refuses. Nothing may be kept.
        when(ai.extract(any())).thenReturn(TestExtractionResults.withLines(List.of(
                new LineItem("A", "Valid line", BigDecimal.ONE, "each", BigDecimal.TEN, BigDecimal.TEN),
                new LineItem("B", "Invalid line", BigDecimal.ZERO, "each", BigDecimal.TEN, BigDecimal.ZERO))));
        UUID id = upload("po-2002.pdf");

        mockMvc.perform(post("/api/documents/{id}/process", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXTRACTION_FAILED"))
                .andExpect(jsonPath("$.failureReason").value("The extraction could not be completed."));

        assertThat(extractions.findByDocumentId(id)).isEmpty();
    }

    @Test
    void onlyOneRequestCanClaimADocumentForExtraction() throws Exception {
        UUID id = upload("po-2003.pdf");

        documentService.startProcessing(id);

        assertThatThrownBy(() -> documentService.startProcessing(id))
                .isInstanceOf(IllegalDocumentStateException.class);
    }

    private UUID upload(String name) throws Exception {
        String body = mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", name, "application/pdf", TestFiles.PDF)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}
