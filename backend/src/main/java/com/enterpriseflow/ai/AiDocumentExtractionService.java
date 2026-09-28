package com.enterpriseflow.ai;

/**
 * Extracts purchase order data from a document. Implementations wrap a specific AI provider; the rest
 * of the application depends only on this interface, so providers can be swapped by configuration.
 */
public interface AiDocumentExtractionService {

    /**
     * @throws AiExtractionException if the provider fails, refuses, times out or returns unusable data
     */
    OrderExtractionResult extract(DocumentInput document);
}
