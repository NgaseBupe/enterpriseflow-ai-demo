package com.enterpriseflow;

import com.enterpriseflow.document.domain.Document;
import com.enterpriseflow.identity.DemoUser;
import java.util.UUID;

/** Builders for synthetic test data. */
public final class TestDocuments {

    private TestDocuments() {
    }

    public static Document pdf() {
        return new Document("po-1001.pdf", "application/pdf", 48_213,
                "a".repeat(64), UUID.randomUUID().toString(), DemoUser.ID);
    }
}
