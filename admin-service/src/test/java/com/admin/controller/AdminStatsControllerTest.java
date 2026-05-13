package com.admin.controller;

import com.admin.client.AuthAdminClient;
import com.admin.dto.AdminStatsResponse;
import com.admin.dto.AdminUserResponse;
import com.admin.security.AdminGuard;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminStatsControllerTest {

    private final AuthAdminClient authAdminClient = mock(AuthAdminClient.class);
    private final AdminGuard adminGuard = mock(AdminGuard.class);
    private final AdminStatsController controller = new AdminStatsController(authAdminClient, adminGuard);

    @Test
    void getStatsBuildsAdminSummaryFromAuthUsers() {
        AdminUserResponse activeUser = new AdminUserResponse();
        activeUser.setActive(true);

        AdminUserResponse inactiveUser = new AdminUserResponse();
        inactiveUser.setActive(false);

        when(authAdminClient.listUsers("Bearer token")).thenReturn(List.of(activeUser, inactiveUser));

        ResponseEntity<AdminStatsResponse> response = controller.getStats("Bearer token", "PLATFORM_ADMIN");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalUsers()).isEqualTo(2);
        assertThat(response.getBody().getActiveUsersToday()).isEqualTo(1);
        assertThat(response.getBody().getTotalWorkspaces()).isEqualTo(12);
        assertThat(response.getBody().getTotalBoards()).isEqualTo(45);
        verify(adminGuard).requirePlatformAdmin("PLATFORM_ADMIN");
    }
}
