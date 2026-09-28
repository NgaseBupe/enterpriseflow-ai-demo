package com.enterpriseflow.document;

import java.util.EnumSet;
import java.util.Set;

/**
 * The lifecycle of a document. Every allowed move is listed here, in one place; anything else is
 * refused.
 */
public enum DocumentStatus {
    UPLOADED("Uploaded"),
    PROCESSING("Processing"),
    EXTRACTED("Extracted"),
    EXTRACTION_FAILED("Extraction failed"),
    IN_REVIEW("In review"),
    CONFIRMED("Confirmed");

    /** States from which extraction may start: a first attempt, or a retry after a failure. */
    public static final Set<DocumentStatus> EXTRACTABLE = EnumSet.of(UPLOADED, EXTRACTION_FAILED);

    private final String label;

    DocumentStatus(String label) {
        this.label = label;
    }

    /** A human-readable name, for messages shown to users. */
    public String label() {
        return label;
    }

    public boolean canTransitionTo(DocumentStatus target) {
        return switch (this) {
            case UPLOADED, EXTRACTION_FAILED -> target == PROCESSING;
            case PROCESSING -> target == EXTRACTED || target == EXTRACTION_FAILED;
            case EXTRACTED -> target == IN_REVIEW || target == CONFIRMED;
            case IN_REVIEW -> target == IN_REVIEW || target == CONFIRMED;
            case CONFIRMED -> false;
        };
    }
}
