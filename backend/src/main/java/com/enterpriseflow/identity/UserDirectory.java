package com.enterpriseflow.identity;

import java.util.UUID;

/** Looks up people for display, e.g. "Confirmed by Demo Reviewer". */
public interface UserDirectory {

    /** The user's display name, or "Unknown user" if the ID does not exist. */
    String displayName(UUID userId);
}
