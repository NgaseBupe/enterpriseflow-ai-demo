package com.enterpriseflow.document.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.enterpriseflow.TestFiles;
import com.enterpriseflow.TestcontainersConfiguration;
import com.enterpriseflow.document.DocumentProperties;
import com.enterpriseflow.document.domain.DocumentRepository;
import com.enterpriseflow.identity.CurrentUser;
import com.enterpriseflow.identity.CurrentUserProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The file is written before the database transaction commits. If the commit then fails, the file
 * must be removed again, or storage fills up with files no document points to.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DocumentUploadRollbackTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    DocumentRepository documents;

    @Autowired
    DocumentProperties properties;

    @MockitoBean
    CurrentUserProvider currentUserProvider;

    @Test
    void removesTheStoredFileWhenTheDatabaseCommitFails() throws Exception {
        // An uploader that doesn't exist violates a foreign key, which MySQL reports at commit time.
        when(currentUserProvider.currentUser()).thenReturn(new CurrentUser(UUID.randomUUID(), "ghost"));
        long documentsBefore = documents.count();
        long filesBefore = storedFileCount();

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "po.pdf", "application/pdf", TestFiles.PDF)))
                .andExpect(status().is5xxServerError());

        assertThat(documents.count()).isEqualTo(documentsBefore);
        assertThat(storedFileCount()).isEqualTo(filesBefore);
    }

    private long storedFileCount() throws Exception {
        Path dir = properties.storageLocation();
        if (!Files.exists(dir)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.count();
        }
    }
}
