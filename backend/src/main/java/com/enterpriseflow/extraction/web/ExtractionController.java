package com.enterpriseflow.extraction.web;

import com.enterpriseflow.document.DocumentView;
import com.enterpriseflow.extraction.ExtractionService;
import com.enterpriseflow.extraction.OrderExtractionView;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents/{documentId}")
class ExtractionController {

    private final ExtractionService extractionService;

    ExtractionController(ExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    /**
     * Runs extraction and returns the document's resulting status. Extraction is synchronous for now;
     * it becomes asynchronous (202 Accepted) when a real AI provider is added in Sprint 5.
     */
    @PostMapping("/process")
    DocumentView process(@PathVariable UUID documentId) {
        return extractionService.extract(documentId);
    }

    @GetMapping("/extraction")
    OrderExtractionView extraction(@PathVariable UUID documentId) {
        return extractionService.getExtraction(documentId);
    }
}
