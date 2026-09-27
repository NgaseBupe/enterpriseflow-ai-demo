package com.enterpriseflow.document.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterpriseflow.TestFiles;
import com.enterpriseflow.TestcontainersConfiguration;
import com.enterpriseflow.audit.AuditEventType;
import com.enterpriseflow.audit.domain.AuditEvent;
import com.enterpriseflow.audit.domain.AuditEventRepository;
import com.enterpriseflow.document.DocumentProperties;
import com.enterpriseflow.document.domain.DocumentRepository;
import com.enterpriseflow.identity.DemoUser;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DocumentUploadIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DocumentRepository documents;

    @Autowired
    AuditEventRepository auditEvents;

    @Autowired
    DocumentProperties properties;

    @Test
    void uploadsAPdfStoresItAndRecordsAnAuditEvent() throws Exception {
        long filesBefore = storedFileCount();

        MvcResult result = mockMvc.perform(multipart("/api/documents").file(file("po-1001.pdf", TestFiles.PDF)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/documents/[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.originalFileName").value("po-1001.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.fileSize").value(TestFiles.PDF.length))
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.uploadedBy").value(DemoUser.ID.toString()))
                .andReturn();

        UUID id = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
        assertThat(documents.findById(id)).isPresent();
        assertThat(storedFileCount()).isEqualTo(filesBefore + 1);
        List<AuditEvent> history = auditEvents.findByDocumentIdOrderByOccurredAtAscIdAsc(id);
        assertThat(history).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo(AuditEventType.DOCUMENT_UPLOADED);
            assertThat(event.getActor()).isEqualTo(DemoUser.USERNAME);
            assertThat(event.getDetails()).containsEntry("fileName", "po-1001.pdf");
        });
    }

    @Test
    void acceptsPngAndJpegImages() throws Exception {
        mockMvc.perform(multipart("/api/documents").file(file("photo.png", TestFiles.PNG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/png"));
        mockMvc.perform(multipart("/api/documents").file(file("scan.jpg", TestFiles.JPEG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));
    }

    @Test
    void detectsTheTypeFromContentNotFromTheClaimedTypeOrName() throws Exception {
        MockMultipartFile disguised = new MockMultipartFile("file", "order.pdf", "application/pdf", TestFiles.PNG);

        mockMvc.perform(multipart("/api/documents").file(disguised))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    void rejectsAnUnsupportedFileAndStoresNothing() throws Exception {
        long documentsBefore = documents.count();
        long filesBefore = storedFileCount();
        MockMultipartFile textPretendingToBePdf =
                new MockMultipartFile("file", "po.pdf", "application/pdf", TestFiles.TEXT);

        mockMvc.perform(multipart("/api/documents").file(textPretendingToBePdf))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unsupported file type"))
                .andExpect(jsonPath("$.detail").value("Only PDF, PNG and JPEG files are accepted."));

        assertThat(documents.count()).isEqualTo(documentsBefore);
        assertThat(storedFileCount()).isEqualTo(filesBefore);
    }

    @Test
    void rejectsAnEmptyFile() throws Exception {
        mockMvc.perform(multipart("/api/documents").file(file("empty.pdf", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Empty file"));
    }

    @Test
    void rejectsAFileOverTheSizeLimit() throws Exception {
        byte[] oversized = new byte[(int) properties.maxFileSize().toBytes() + 1];
        System.arraycopy(TestFiles.PDF, 0, oversized, 0, TestFiles.PDF.length);

        mockMvc.perform(multipart("/api/documents").file(file("huge.pdf", oversized)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.detail").value("The file exceeds the maximum size of 10 MB."));
    }

    @Test
    void rejectsARequestWithoutAFile() throws Exception {
        mockMvc.perform(multipart("/api/documents"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void storesOnlyTheLastSegmentOfAClientPath() throws Exception {
        mockMvc.perform(multipart("/api/documents").file(file("C:\\Users\\me\\po-1002.pdf", TestFiles.PDF)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalFileName").value("po-1002.pdf"));
    }

    @Test
    void returnsAnUploadedDocument() throws Exception {
        MvcResult created = mockMvc.perform(multipart("/api/documents").file(file("po-1003.pdf", TestFiles.PDF)))
                .andReturn();
        String location = created.getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFileName").value("po-1003.pdf"))
                .andExpect(jsonPath("$.status").value("UPLOADED"));
    }

    @Test
    void returnsNotFoundForAnUnknownDocument() throws Exception {
        mockMvc.perform(get("/api/documents/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Document not found"));
    }

    @Test
    void returnsBadRequestForAMalformedId() throws Exception {
        mockMvc.perform(get("/api/documents/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, MediaType.APPLICATION_OCTET_STREAM_VALUE, content);
    }

    private long storedFileCount() throws IOException {
        if (!Files.exists(properties.storageLocation())) {
            return 0;
        }
        try (Stream<java.nio.file.Path> files = Files.list(properties.storageLocation())) {
            return files.count();
        }
    }
}
