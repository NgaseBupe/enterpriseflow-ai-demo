package com.enterpriseflow.document.domain;

import com.enterpriseflow.document.DocumentStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Moves a document to PROCESSING only if it is currently in one of {@code from}, as a single
     * atomic UPDATE. When two requests race, exactly one gets 1 row updated; the other gets 0.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Document d
               set d.status = com.enterpriseflow.document.DocumentStatus.PROCESSING, d.failureReason = null
             where d.id = :id and d.status in :from
            """)
    int claimForProcessing(@Param("id") UUID id, @Param("from") Collection<DocumentStatus> from);
}
