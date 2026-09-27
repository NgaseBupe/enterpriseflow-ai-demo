package com.enterpriseflow.extraction.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderExtractionRepository extends JpaRepository<OrderExtraction, UUID> {

    /** Loads the extraction together with its lines in a single query. */
    @EntityGraph(attributePaths = "lines")
    Optional<OrderExtraction> findByDocumentId(UUID documentId);
}
