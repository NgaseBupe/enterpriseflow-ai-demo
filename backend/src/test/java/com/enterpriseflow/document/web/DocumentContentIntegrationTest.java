package com.enterpriseflow.document.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterpriseflow.TestFiles;
import com.enterpriseflow.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DocumentContentIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void servesTheOriginalFileInlineWithItsDetectedType() throws Exception {
        UUID id = upload("po-1001.pdf", TestFiles.PDF);

        mockMvc.perform(get("/api/documents/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(TestFiles.PDF))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(header().string("Content-Disposition", containsString("po-1001.pdf")));
    }

    @Test
    void usesTheTypeDetectedAtUploadNotTheNameOrClaimedType() throws Exception {
        UUID id = upload("looks-like.pdf", TestFiles.PNG);

        mockMvc.perform(get("/api/documents/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void sendsHeadersThatStopTheFileBeingMisusedByTheBrowser() throws Exception {
        UUID id = upload("po-1002.pdf", TestFiles.PDF);

        mockMvc.perform(get("/api/documents/{id}/content", id))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'self'"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    void namesThePlainFileInTheStandardFormat() throws Exception {
        UUID id = upload("po-3101.pdf", TestFiles.PDF);

        mockMvc.perform(get("/api/documents/{id}/content", id))
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"po-3101.pdf\"; filename*=UTF-8''po-3101.pdf"));
    }

    @Test
    void encodesUnusualFileNamesSafelyInTheHeader() throws Exception {
        // RFC 6266: an ASCII-only fallback in "filename", the exact name percent-encoded in "filename*".
        UUID id = upload("order \"draft\"; ünïcode.pdf", TestFiles.PDF);

        mockMvc.perform(get("/api/documents/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "inline; filename=\"order _draft_; _n_code.pdf\"; "
                                + "filename*=UTF-8''order%20%22draft%22%3B%20%C3%BCn%C3%AFcode.pdf"));
    }

    @Test
    void returnsNotFoundForAnUnknownDocument() throws Exception {
        mockMvc.perform(get("/api/documents/{id}/content", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Document not found"));
    }

    private UUID upload(String name, byte[] content) throws Exception {
        String body = mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", name, "application/octet-stream", content)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}
