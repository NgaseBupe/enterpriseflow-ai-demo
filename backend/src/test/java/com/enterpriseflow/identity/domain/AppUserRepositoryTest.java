package com.enterpriseflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.enterpriseflow.RepositoryTest;
import com.enterpriseflow.identity.DemoUser;
import com.enterpriseflow.identity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@RepositoryTest
class AppUserRepositoryTest {

    @Autowired
    AppUserRepository users;

    @Test
    void demoUserIsSeededAndCannotSignIn() {
        AppUser demo = users.findById(DemoUser.ID).orElseThrow();

        assertThat(demo.getUsername()).isEqualTo(DemoUser.USERNAME);
        assertThat(demo.getRole()).isEqualTo(Role.REVIEWER);
        assertThat(demo.getPasswordHash()).isEqualTo("!");
    }

    @Test
    void usernamesAreUnique() {
        AppUser duplicate = new AppUser(DemoUser.USERNAME, "hash", "Someone Else", Role.ADMIN);

        assertThatThrownBy(() -> users.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_app_user_username");
    }
}
