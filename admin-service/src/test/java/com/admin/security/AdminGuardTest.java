package com.admin.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminGuardTest {

    private final AdminGuard adminGuard = new AdminGuard();

    @Test
    void allowsPlatformAdminRoleCaseInsensitively() {
        assertThatCode(() -> adminGuard.requirePlatformAdmin("platform_admin")).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingOrNonAdminRole() {
        assertThatThrownBy(() -> adminGuard.requirePlatformAdmin(null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> adminGuard.requirePlatformAdmin("USER"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Platform admin access required");
    }
}
