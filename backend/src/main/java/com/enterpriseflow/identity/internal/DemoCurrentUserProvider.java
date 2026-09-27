package com.enterpriseflow.identity.internal;

import com.enterpriseflow.identity.CurrentUser;
import com.enterpriseflow.identity.CurrentUserProvider;
import com.enterpriseflow.identity.DemoUser;
import org.springframework.stereotype.Component;

@Component
class DemoCurrentUserProvider implements CurrentUserProvider {

    private static final CurrentUser DEMO = new CurrentUser(DemoUser.ID, DemoUser.USERNAME);

    @Override
    public CurrentUser currentUser() {
        return DEMO;
    }
}
