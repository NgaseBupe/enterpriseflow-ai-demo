package com.enterpriseflow.document;

import java.time.Instant;
import java.util.UUID;

/** Read-only view of a document, exposed to other modules and the web layer. */
public record DocumentView(
        UUID id,
        String originalFileName,
        String contentType,
        long fileSize,
        DocumentStatus status,
        String failureReason,
        Instant uploadedAt,
        UUID uploadedBy) {
}
