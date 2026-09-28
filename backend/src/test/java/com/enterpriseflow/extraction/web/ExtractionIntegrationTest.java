package com.enterpriseflow.extraction.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterpriseflow.TestFiles;
import com.enterpriseflow.TestcontainersConfiguration;
import com.enterpriseflow.audit.AuditEventType;
import com.enterpriseflow.audit.domain.AuditEvent;
import com.enterpriseflow.audit.domain.AuditEventRepository;
import com.enterpriseflow.extraction.domain.OrderExtractionRepository;
import com.enterpriseflow.identity.DemoUser;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** Extraction end to end, with the built-in mock AI provider. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExtractionIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AuditEventRepository auditEvents;

    @Autowired
    OrderExtractionRepository extractions;

    @Test
    void extractsTheOrderHeaderAndLinesAndRecordsTheOutcome() throws Exception {
        UUID id = upload("po-1001.pdf");

        mockMvc.perform(post("/api/documents/{id}/process", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXTRACTED"))
                .andExpect(jsonPath("$.failureReason").doesNotExist());

        mockMvc.perform(get("/api/documents/{id}/extraction", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(id.toString()))
                .andExpect(jsonPath("$.poNumber").value("PO-1001"))
                .andExpect(jsonPath("$.currency").value("ZMW"))
                .andExpect(jsonPath("$.lines.length()").value(3))
                .andExpect(jsonPath("$.lines[0].lineNumber").value(1))
                .andExpect(jsonPath("$.lines[0].description").value("M8 hex bolts, box of 100"))
                .andExpect(jsonPath("$.aiProvider").value("mock"))
                .andExpect(jsonPath("$.aiModel").value("mock-v1"));

        assertThat(auditEvents.findByDocumentIdOrderByOccurredAtAscIdAsc(id))
                .extracting(AuditEvent::getEventType, AuditEvent::getActor)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(AuditEventType.DOCUMENT_UPLOADED, DemoUser.USERNAME),
                        org.assertj.core.groups.Tuple.tuple(AuditEventType.EXTRACTION_REQUESTED, DemoUser.USERNAME),
                        org.assertj.core.groups.Tuple.tuple(AuditEventType.EXTRACTION_SUCCEEDED, "SYSTEM"));
    }

    @Test
    void refusesToExtractADocumentThatIsAlreadyExtracted() throws Exception {
        UUID id = upload("po-1002.pdf");
        mockMvc.perform(post("/api/documents/{id}/process", id)).andExpect(status().isOk());

        mockMvc.perform(post("/api/documents/{id}/process", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("EXTRACTED"))
                .andExpect(jsonPath("$.detail").value("Cannot start extraction while the document's status is \"Extracted\"."));
    }

    @Test
    void recordsAProviderFailureWithAReadableReasonAndNoData() throws Exception {
        UUID id = upload("po-fail-1003.pdf");

        mockMvc.perform(post("/api/documents/{id}/process", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXTRACTION_FAILED"))
                .andExpect(jsonPath("$.failureReason").value("The AI service returned an error."));

        assertThat(extractions.findByDocumentId(id)).isEmpty();
        mockMvc.perform(get("/api/documents/{id}/extraction", id)).andExpect(status().isNotFound());
        java.util.List<AuditEvent> history = auditEvents.findByDocumentIdOrderByOccurredAtAscIdAsc(id);
        AuditEvent last = history.get(history.size() - 1);
        assertThat(last.getEventType()).isEqualTo(AuditEventType.EXTRACTION_FAILED);
        assertThat(last.getDetails()).containsEntry("reason", "PROVIDER_ERROR");
    }

    @Test
    void reportsThatADocumentHasNotBeenExtractedYet() throws Exception {
        UUID id = upload("po-1004.pdf");

        mockMvc.perform(get("/api/documents/{id}/extraction", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Extraction not found"));
    }

    @Test
    void returnsNotFoundForAnUnknownDocument() throws Exception {
        UUID unknown = UUID.randomUUID();

        mockMvc.perform(post("/api/documents/{id}/process", unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Document not found"));
        mockMvc.perform(get("/api/documents/{id}/extraction", unknown))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Document not found"));
    }

    private UUID upload(String name) throws Exception {
        String body = mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", name, "application/pdf", TestFiles.PDF)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}
