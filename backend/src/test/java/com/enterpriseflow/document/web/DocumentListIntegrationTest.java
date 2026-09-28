package com.enterpriseflow.document.web;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterpriseflow.TestFiles;
import com.enterpriseflow.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DocumentListIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void listsDocumentsNewestFirstWithPageInformation() throws Exception {
        upload("older.pdf");
        upload("newer.pdf");

        mockMvc.perform(get("/api/documents").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].originalFileName").value("newer.pdf"))
                .andExpect(jsonPath("$.items[1].originalFileName").value("older.pdf"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalItems").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalPages").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void returnsLaterPages() throws Exception {
        upload("page-test-1.pdf");
        upload("page-test-2.pdf");

        mockMvc.perform(get("/api/documents").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].originalFileName").value("page-test-1.pdf"))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void usesSensibleDefaults() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void rejectsInvalidPageParameters() throws Exception {
        mockMvc.perform(get("/api/documents").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mockMvc.perform(get("/api/documents").param("size", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/documents").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/documents").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    private void upload(String name) throws Exception {
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", name, "application/pdf", TestFiles.PDF)))
                .andExpect(status().isCreated());
    }
}
