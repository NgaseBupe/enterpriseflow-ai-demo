package com.enterpriseflow.document.web;

import com.enterpriseflow.common.PageResponse;
import com.enterpriseflow.document.DocumentContent;
import com.enterpriseflow.document.DocumentService;
import com.enterpriseflow.document.DocumentView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

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

    /**
     * Serves the original file so the reviewer can see it next to the extracted data.
     *
     * <p>The content type is the one detected from the file's signature at upload, never the client's.
     * {@code nosniff} stops browsers guessing a different type, and the content security policy stops the
     * file running scripts or loading anything else, while still letting this application's own pages
     * embed it.
     */
    @GetMapping("/{id}/content")
    ResponseEntity<byte[]> content(@PathVariable UUID id) {
        DocumentContent file = documentService.loadContent(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, inlineDisposition(file.fileName()))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'self'")
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(file.content());
    }

    /**
     * Builds the header as RFC 6266 recommends: an ASCII-only fallback in {@code filename} for old clients,
     * and the exact name, percent-encoded as UTF-8, in {@code filename*}. Spring's own builder writes an
     * email-style (RFC 2047) value into {@code filename}, which RFC 6266 advises against.
     */
    static String inlineDisposition(String fileName) {
        String asciiFallback = fileName.replaceAll("[^\\x20-\\x7E]|[\"\\\\]", "_");
        return "inline; filename=\"" + asciiFallback + "\"; filename*=UTF-8''"
                + UriUtils.encode(fileName, StandardCharsets.UTF_8);
    }
}
