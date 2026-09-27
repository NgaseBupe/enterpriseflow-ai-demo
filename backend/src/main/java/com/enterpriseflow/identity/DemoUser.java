package com.enterpriseflow.identity;

import java.util.UUID;

/**
 * The fixed user that actions are attributed to until login arrives in Sprint 3.
 * Seeded by migration {@code V2__seed_demo_user.sql}; the account cannot be used to sign in.
 */
public final class DemoUser {

    public static final UUID ID = UUID.fromString("01920000-0000-7000-8000-000000000001");
    public static final String USERNAME = "demo.reviewer";

    private DemoUser() {
    }
}
