package com.enterpriseflow.document;

import com.enterpriseflow.audit.AuditEventType;
import com.enterpriseflow.audit.AuditService;
import com.enterpriseflow.common.PageResponse;
import com.enterpriseflow.document.domain.Document;
import com.enterpriseflow.document.domain.DocumentRepository;
import com.enterpriseflow.document.internal.FileNames;
import com.enterpriseflow.document.internal.FileSignature;
import com.enterpriseflow.document.storage.DocumentStorage;
import com.enterpriseflow.identity.CurrentUser;
import com.enterpriseflow.identity.CurrentUserProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DocumentService {

    private final DocumentRepository documents;
    private final DocumentStorage storage;
    private final AuditService audit;
    private final CurrentUserProvider currentUserProvider;
    private final DocumentProperties properties;

    DocumentService(DocumentRepository documents, DocumentStorage storage, AuditService audit,
                    CurrentUserProvider currentUserProvider, DocumentProperties properties) {
        this.documents = documents;
        this.storage = storage;
        this.audit = audit;
        this.currentUserProvider = currentUserProvider;
        this.properties = properties;
    }

    /**
     * Validates and stores an uploaded file, and records the upload in the audit trail.
     *
     * @throws DocumentRejectedException if the file is empty, too large or not a PDF, PNG or JPEG
     */
    @Transactional
    public DocumentView upload(String originalFileName, byte[] content) {
        if (content.length == 0) {
            throw DocumentRejectedException.empty();
        }
        long maxBytes = properties.maxFileSize().toBytes();
        if (content.length > maxBytes) {
            throw DocumentRejectedException.tooLarge(maxBytes);
        }
        FileSignature type = FileSignature.detect(content)
                .orElseThrow(DocumentRejectedException::unsupportedType);

        CurrentUser user = currentUserProvider.currentUser();
        String storageKey = UUID.randomUUID().toString();
        Document document = documents.save(new Document(FileNames.sanitize(originalFileName), type.mediaType(),
                content.length, sha256(content), storageKey, user.id()));
        audit.record(document.getId(), AuditEventType.DOCUMENT_UPLOADED, user.username(), Map.of(
                "fileName", document.getOriginalFileName(),
                "contentType", document.getContentType(),
                "fileSize", document.getFileSize()));

        storage.store(storageKey, content);
        deleteFileIfTransactionRollsBack(storageKey);
        return toView(document);
    }

    /** Newest first; the time-ordered ID breaks ties between documents uploaded in the same instant. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("uploadedAt"), Sort.Order.desc("id"));

    @Transactional(readOnly = true)
    public PageResponse<DocumentView> list(int page, int size) {
        return PageResponse.from(documents.findAll(PageRequest.of(page, size, NEWEST_FIRST)), DocumentService::toView);
    }

    @Transactional(readOnly = true)
    public DocumentView get(UUID id) {
        return documents.findById(id)
                .map(DocumentService::toView)
                .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    /** The file is written before the transaction commits; if the commit fails, remove it again. */
    private void deleteFileIfTransactionRollsBack(String storageKey) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    storage.delete(storageKey);
                }
            }
        });
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    private static DocumentView toView(Document document) {
        return new DocumentView(document.getId(), document.getOriginalFileName(), document.getContentType(),
                document.getFileSize(), document.getStatus(), document.getFailureReason(),
                document.getUploadedAt(), document.getUploadedBy());
    }
}
