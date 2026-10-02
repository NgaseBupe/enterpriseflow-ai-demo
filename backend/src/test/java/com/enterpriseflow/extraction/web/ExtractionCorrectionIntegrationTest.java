package com.enterpriseflow.extraction.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
import com.fasterxml.jackson.databind.node.ArrayNode;
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
class ExtractionCorrectionIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    AuditEventRepository auditEvents;

    @Test
    void savesCorrectionsMovesTheDocumentToInReviewAndRecordsWhatChanged() throws Exception {
        UUID id = extractedDocument("po-5001.pdf");
        ObjectNode request = currentExtraction(id);
        request.put("customerName", "Chanda Hardware Limited");
        line(request, 2).put("quantity", 11);

        save(id, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Chanda Hardware Limited"))
                .andExpect(jsonPath("$.lines[1].quantity").value(11))
                .andExpect(jsonPath("$.version").value(request.get("version").asLong() + 1));

        mockMvc.perform(get("/api/documents/{id}", id)).andExpect(jsonPath("$.status").value("IN_REVIEW"));
        AuditEvent edited = lastEvent(id);
        assertThat(edited.getEventType()).isEqualTo(AuditEventType.EXTRACTION_EDITED);
        assertThat(edited.getActor()).isEqualTo(DemoUser.USERNAME);
        assertThat(edited.getDetails().get("changedFields"))
                .asList().containsExactly("customerName", "lines[2].quantity");
    }

    @Test
    void canBeCorrectedAgainWhileInReview() throws Exception {
        UUID id = extractedDocument("po-5002.pdf");
        ObjectNode first = currentExtraction(id);
        first.put("poNumber", "PO-5002-A");
        save(id, first).andExpect(status().isOk());

        ObjectNode second = currentExtraction(id);
        second.put("poNumber", "PO-5002-B");

        save(id, second).andExpect(status().isOk()).andExpect(jsonPath("$.poNumber").value("PO-5002-B"));
    }

    @Test
    void savingWithoutChangesRecordsNothing() throws Exception {
        UUID id = extractedDocument("po-5003.pdf");
        ObjectNode unchanged = currentExtraction(id);
        line(unchanged, 1).put("unitPrice", 85); // same value as 85.00

        save(id, unchanged).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(unchanged.get("version").asLong()));

        mockMvc.perform(get("/api/documents/{id}", id)).andExpect(jsonPath("$.status").value("EXTRACTED"));
        assertThat(lastEvent(id).getEventType()).isEqualTo(AuditEventType.EXTRACTION_SUCCEEDED);
    }

    @Test
    void treatsBlankTextAsEmpty() throws Exception {
        UUID id = extractedDocument("po-5004.pdf");
        ObjectNode request = currentExtraction(id);
        request.put("notes", "   ");

        save(id, request).andExpect(status().isOk()).andExpect(jsonPath("$.notes").doesNotExist());
    }

    @Test
    void rejectsInvalidValuesWithAMessagePerField() throws Exception {
        UUID id = extractedDocument("po-5005.pdf");
        ObjectNode request = currentExtraction(id);
        request.put("customerEmail", "not-an-email");
        request.put("currency", "zmw");
        line(request, 1).put("quantity", -1);

        save(id, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid input"))
                .andExpect(jsonPath("$.errors[*].field")
                        .value(containsInAnyOrder("customerEmail", "currency", "lines[0].quantity")))
                .andExpect(jsonPath("$.errors[?(@.field == 'lines[0].quantity')].message").value("must be greater than 0"));

        mockMvc.perform(get("/api/documents/{id}", id)).andExpect(jsonPath("$.status").value("EXTRACTED"));
    }

    @Test
    void rejectsAnEditBasedOnAnOutdatedVersion() throws Exception {
        UUID id = extractedDocument("po-5006.pdf");
        ObjectNode reviewerA = currentExtraction(id);
        ObjectNode reviewerB = currentExtraction(id);
        reviewerA.put("customerPhone", "+260 977 000 001");
        save(id, reviewerA).andExpect(status().isOk());

        reviewerB.put("customerPhone", "+260 977 000 002");

        save(id, reviewerB)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Changed by someone else"));
    }

    @Test
    void requiresExactlyTheExistingLines() throws Exception {
        UUID id = extractedDocument("po-5007.pdf");
        ObjectNode missingLine = currentExtraction(id);
        ((ArrayNode) missingLine.get("lines")).remove(2);
        ObjectNode duplicateLine = currentExtraction(id);
        line(duplicateLine, 3).put("lineNumber", 1);

        save(id, missingLine).andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Lines do not match"));
        save(id, duplicateLine).andExpect(status().isBadRequest()).andExpect(jsonPath("$.title").value("Lines do not match"));
    }

    @Test
    void refusesEditsBeforeExtraction() throws Exception {
        UUID id = upload("po-5008.pdf");
        ObjectNode request = json.createObjectNode().put("version", 0);
        request.putArray("lines");

        save(id, request)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentStatus").value("UPLOADED"));
    }

    @Test
    void returnsNotFoundForAnUnknownDocument() throws Exception {
        ObjectNode request = json.createObjectNode().put("version", 0);
        request.putArray("lines");

        save(UUID.randomUUID(), request).andExpect(status().isNotFound());
    }

    @Test
    void requiresTheVersion() throws Exception {
        UUID id = extractedDocument("po-5009.pdf");
        ObjectNode request = currentExtraction(id);
        request.remove("version");

        save(id, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("version"));
    }

    private ResultActions save(UUID id, ObjectNode request) throws Exception {
        return mockMvc.perform(put("/api/documents/{id}/extraction", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)));
    }

    /** The current extraction, as the review screen would send it back. */
    private ObjectNode currentExtraction(UUID id) throws Exception {
        String body = mockMvc.perform(get("/api/documents/{id}/extraction", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return (ObjectNode) json.readTree(body);
    }

    private static ObjectNode line(ObjectNode request, int lineNumber) {
        return (ObjectNode) request.get("lines").get(lineNumber - 1);
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
