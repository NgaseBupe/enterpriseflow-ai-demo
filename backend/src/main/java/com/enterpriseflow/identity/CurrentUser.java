package com.enterpriseflow.identity;

import java.util.UUID;

/** The user performing the current request. */
public record CurrentUser(UUID id, String username) {
}
