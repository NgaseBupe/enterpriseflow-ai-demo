package com.enterpriseflow.document.domain;

import com.enterpriseflow.common.BaseEntity;
import com.enterpriseflow.document.DocumentStatus;
import com.enterpriseflow.document.IllegalDocumentStateException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An uploaded file and its processing status. The file itself lives in document storage. */
@Entity
@Table(name = "document")
public class Document extends BaseEntity {

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 64, columnDefinition = "char(64)")
    private String sha256;

    @Column(name = "storage_key", nullable = false, length = 100)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private DocumentStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    /** References the uploading user by ID only; users belong to the identity module. */
    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    protected Document() {
    }

    public Document(String originalFileName, String contentType, long fileSize, String sha256,
                    String storageKey, UUID uploadedBy) {
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.sha256 = sha256;
        this.storageKey = storageKey;
        this.uploadedBy = uploadedBy;
        this.status = DocumentStatus.UPLOADED;
        this.uploadedAt = Instant.now();
    }

    public void markExtracted() {
        transitionTo(DocumentStatus.EXTRACTED, "complete extraction");
        this.failureReason = null;
    }

    public void markExtractionFailed(String reason) {
        transitionTo(DocumentStatus.EXTRACTION_FAILED, "record an extraction failure");
        this.failureReason = reason;
    }

    private void transitionTo(DocumentStatus target, String action) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalDocumentStateException(status, action);
        }
        this.status = target;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getSha256() {
        return sha256;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public UUID getUploadedBy() {
        return uploadedBy;
    }
}
