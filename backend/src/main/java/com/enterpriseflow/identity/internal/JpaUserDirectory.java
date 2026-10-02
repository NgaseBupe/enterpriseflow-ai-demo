package com.enterpriseflow.identity.internal;

import com.enterpriseflow.identity.UserDirectory;
import com.enterpriseflow.identity.domain.AppUser;
import com.enterpriseflow.identity.domain.AppUserRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaUserDirectory implements UserDirectory {

    static final String UNKNOWN = "Unknown user";

    private final AppUserRepository users;

    JpaUserDirectory(AppUserRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public String displayName(UUID userId) {
        return users.findById(userId).map(AppUser::getDisplayName).orElse(UNKNOWN);
    }
}
