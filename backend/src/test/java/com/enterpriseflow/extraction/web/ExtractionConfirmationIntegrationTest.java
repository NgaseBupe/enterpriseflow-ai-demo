package com.enterpriseflow.extraction.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterpriseflow.TestFiles;
import com.enterpriseflow.TestcontainersConfiguration;
import com.enterpriseflow.audit.AuditEventType;
import com.enterpriseflow.audit.domain.AuditEvent;
import com.enterpriseflow.audit.domain.AuditEventRepository;
import com.enterpriseflow.identity.DemoUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExtractionConfirmationIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    AuditEventRepository auditEvents;

    @Test
    void confirmsTheExtractionAndRecordsWhoDidItAndWhen() throws Exception {
        UUID id = extractedDocument("po-6001.pdf");

        confirm(id, currentVersion(id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewedBy").value(DemoUser.ID.toString()))
                .andExpect(jsonPath("$.reviewedByName").value("Demo Reviewer"))
                .andExpect(jsonPath("$.reviewedAt").exists());

        mockMvc.perform(get("/api/documents/{id}", id)).andExpect(jsonPath("$.status").value("CONFIRMED"));
        AuditEvent confirmed = lastEvent(id);
        assertThat(confirmed.getEventType()).isEqualTo(AuditEventType.EXTRACTION_CONFIRMED);
        assertThat(confirmed.getActor()).isEqualTo(DemoUser.USERNAME);
    }

    @Test
    void canConfirmAfterCorrections() throws Exception {
        UUID id = extractedDocument("po-6002.pdf");
        ObjectNode corrected = currentExtraction(id);
        corrected.put("customerName", "Mwila Building Supplies");
        save(id, corrected).andExpect(status().isOk());

        confirm(id, currentVersion(id)).andExpect(status().isOk());

        mockMvc.perform(get("/api/documents/{id}/extraction", id))
                .andExpect(jsonPath("$.customerName").value("Mwila Building Supplies"))
                .andExpect(jsonPath("$.reviewedByName").value("Demo Reviewer"));
    }

    @Test
    void aConfirmedExtractionCannotBeEditedEvenWithoutChanges() throws Exception {
        // Review finding R-027: an unchanged save used to succeed on a confirmed extraction.
        UUID id = extractedDocument("po-6003.pdf");
        confirm(id, currentVersion(id)).andExpect(status().isOk());
        ObjectNode unchanged = currentExtraction(id);
        ObjectNode changed = currentExtraction(id);
        changed.put("poNumber", "PO-CHANGED");

        save(id, unchanged).andExpect(status().isConflict()).andExpect(jsonPath("$.currentStatus").value("CONFIRMED"));
        save(id, changed).andExpect(status().isConflict());
        mockMvc.perform(get("/api/documents/{id}/extraction", id)).andExpect(jsonPath("$.poNumber").value("PO-6003"));
    }

    @Test
    void cannotBeConfirmedTwiceOrExtractedAgain() throws Exception {
        UUID id = extractedDocument("po-6004.pdf");
        confirm(id, currentVersion(id)).andExpect(status().isOk());

        confirm(id, currentVersion(id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot confirm the extracted data while the document's status is \"Confirmed\"."));
        mockMvc.perform(post("/api/documents/{id}/process", id)).andExpect(status().isConflict());
    }

    @Test
    void refusesToConfirmDataThatChangedAfterTheReviewerLookedAtIt() throws Exception {
        UUID id = extractedDocument("po-6005.pdf");
        long versionTheReviewerSaw = currentVersion(id);
        ObjectNode colleaguesEdit = currentExtraction(id);
        colleaguesEdit.put("customerPhone", "+260 977 000 003");
        save(id, colleaguesEdit).andExpect(status().isOk());

        confirm(id, versionTheReviewerSaw)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Changed by someone else"));

        mockMvc.perform(get("/api/documents/{id}", id)).andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test
    void cannotConfirmBeforeExtraction() throws Exception {
        UUID id = upload("po-6006.pdf");

        confirm(id, 0).andExpect(status().isConflict()).andExpect(jsonPath("$.currentStatus").value("UPLOADED"));
    }

    @Test
    void requiresTheVersion() throws Exception {
        UUID id = extractedDocument("po-6007.pdf");

        mockMvc.perform(post("/api/documents/{id}/review/confirm", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("version"));
    }

    @Test
    void returnsNotFoundForAnUnknownDocument() throws Exception {
        confirm(UUID.randomUUID(), 0).andExpect(status().isNotFound());
    }

    private ResultActions confirm(UUID id, long version) throws Exception {
        return mockMvc.perform(post("/api/documents/{id}/review/confirm", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": " + version + "}"));
    }

    private ResultActions save(UUID id, ObjectNode request) throws Exception {
        return mockMvc.perform(put("/api/documents/{id}/extraction", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)));
    }

    private ObjectNode currentExtraction(UUID id) throws Exception {
        String body = mockMvc.perform(get("/api/documents/{id}/extraction", id))
                .andReturn().getResponse().getContentAsString();
        return (ObjectNode) json.readTree(body);
    }

    private long currentVersion(UUID id) throws Exception {
        return currentExtraction(id).get("version").asLong();
    }

    private AuditEvent lastEvent(UUID id) {
        List<AuditEvent> history = auditEvents.findByDocumentIdOrderByOccurredAtAscIdAsc(id);
        return history.get(history.size() - 1);
    }

    private UUID extractedDocument(String name) throws Exception {
        UUID id = upload(name);
        mockMvc.perform(post("/api/documents/{id}/process", id)).andExpect(jsonPath("$.status").value("EXTRACTED"));
        return id;
    }

    private UUID upload(String name) throws Exception {
        String body = mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", name, "application/pdf", TestFiles.PDF)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}
