package com.enterpriseflow.identity;

/**
 * Supplies the user behind the current request. Until login arrives in Sprint 3 this is always the
 * demo user; afterwards it will read the authenticated user from the security context.
 */
public interface CurrentUserProvider {

    CurrentUser currentUser();
}
