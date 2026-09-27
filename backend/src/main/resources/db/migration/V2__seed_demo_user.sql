-- Fixed demo user that actions are attributed to until login arrives (Sprint 3).
-- The password hash '!' matches no password, so this account can never be used to sign in.
INSERT INTO app_user (id, username, password_hash, display_name, role, enabled, created_at)
VALUES (UUID_TO_BIN('01920000-0000-7000-8000-000000000001'),
        'demo.reviewer', '!', 'Demo Reviewer', 'REVIEWER', TRUE, UTC_TIMESTAMP(6));
