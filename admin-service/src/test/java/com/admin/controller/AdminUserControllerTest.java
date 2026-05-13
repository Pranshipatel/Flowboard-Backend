package com.admin.controller;

import com.admin.client.AuthAdminClient;
import com.admin.dto.AdminUserResponse;
import com.admin.security.AdminGuard;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserControllerTest {

    private final AuthAdminClient authAdminClient = mock(AuthAdminClient.class);
    private final AdminGuard adminGuard = mock(AdminGuard.class);
    private final AdminUserController controller = new AdminUserController(authAdminClient, adminGuard);

    @Test
    void listUsersDelegatesToAuthService() {
        AdminUserResponse user = new AdminUserResponse();
        when(authAdminClient.listUsers("Bearer token")).thenReturn(List.of(user));

        ResponseEntity<List<AdminUserResponse>> response = controller.listUsers("Bearer token", "PLATFORM_ADMIN");

        assertThat(response.getBody()).containsExactly(user);
        verify(adminGuard).requirePlatformAdmin("PLATFORM_ADMIN");
    }

    @Test
    void getUserDelegatesToAuthService() {
        AdminUserResponse user = new AdminUserResponse();
        when(authAdminClient.getUser(7L, "Bearer token")).thenReturn(user);

        ResponseEntity<AdminUserResponse> response = controller.getUser(7L, "Bearer token", "PLATFORM_ADMIN");

        assertThat(response.getBody()).isSameAs(user);
        verify(adminGuard).requirePlatformAdmin("PLATFORM_ADMIN");
    }

    @Test
    void deleteUserDelegatesToAuthService() {
        when(authAdminClient.deleteUser(7L, "Bearer token")).thenReturn("deleted");

        ResponseEntity<String> response = controller.deleteUser(7L, "Bearer token", "PLATFORM_ADMIN");

        assertThat(response.getBody()).isEqualTo("deleted");
        verify(adminGuard).requirePlatformAdmin("PLATFORM_ADMIN");
    }

    @Test
    void updateUserRoleDelegatesToAuthService() {
        when(authAdminClient.updateUserRole(7L, "USER", "Bearer token")).thenReturn("updated");

        ResponseEntity<String> response = controller.updateUserRole(7L, "USER", "Bearer token", "PLATFORM_ADMIN");

        assertThat(response.getBody()).isEqualTo("updated");
        verify(adminGuard).requirePlatformAdmin("PLATFORM_ADMIN");
    }

    @Test
    void suspendUserDelegatesToAuthService() {
        when(authAdminClient.suspendUser(7L, "Bearer token")).thenReturn("suspended");

        ResponseEntity<String> response = controller.suspendUser(7L, "Bearer token", "PLATFORM_ADMIN");

        assertThat(response.getBody()).isEqualTo("suspended");
        verify(adminGuard).requirePlatformAdmin("PLATFORM_ADMIN");
    }

    @Test
    void reactivateUserDelegatesToAuthService() {
        when(authAdminClient.reactivateUser(7L, "Bearer token")).thenReturn("reactivated");

        ResponseEntity<String> response = controller.reactivateUser(7L, "Bearer token", "PLATFORM_ADMIN");

        assertThat(response.getBody()).isEqualTo("reactivated");
        verify(adminGuard).requirePlatformAdmin("PLATFORM_ADMIN");
    }
}
