package com.enterpriseflow.document.web;

import com.enterpriseflow.common.PageResponse;
import com.enterpriseflow.document.DocumentService;
import com.enterpriseflow.document.DocumentView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
class DocumentController {

    private final DocumentService documentService;

    DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentView> upload(@RequestParam("file") MultipartFile file) throws IOException {
        DocumentView document = documentService.upload(file.getOriginalFilename(), file.getBytes());
        return ResponseEntity.created(URI.create("/api/documents/" + document.id())).body(document);
    }

    @GetMapping
    PageResponse<DocumentView> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                    @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return documentService.list(page, size);
    }

    @GetMapping("/{id}")
    DocumentView get(@PathVariable UUID id) {
        return documentService.get(id);
    }
}
