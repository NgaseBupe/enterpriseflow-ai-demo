package com.enterpriseflow.extraction;

import jakarta.validation.constraints.NotNull;

/**
 * Confirms the extraction exactly as the reviewer saw it: if someone changed it in the meantime, the
 * version no longer matches and the confirmation is refused.
 */
public record ConfirmExtractionRequest(@NotNull Long version) {
}
