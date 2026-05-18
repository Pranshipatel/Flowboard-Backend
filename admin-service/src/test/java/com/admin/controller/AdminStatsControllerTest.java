package com.admin.controller;

import com.admin.client.AuthAdminClient;
import com.admin.client.PlatformAdminClient;
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
    private final PlatformAdminClient platformAdminClient = mock(PlatformAdminClient.class);
    private final AdminGuard adminGuard = mock(AdminGuard.class);
    private final AdminStatsController controller = new AdminStatsController(authAdminClient, platformAdminClient, adminGuard);

    @Test
    void getStatsBuildsAdminSummaryFromAuthUsers() {
        AdminUserResponse activeUser = new AdminUserResponse();
        activeUser.setActive(true);

        AdminUserResponse inactiveUser = new AdminUserResponse();
        inactiveUser.setActive(false);

        when(authAdminClient.listUsers("Bearer token")).thenReturn(List.of(activeUser, inactiveUser));
        when(platformAdminClient.listWorkspaces("Bearer token")).thenReturn(List.of(new Object(), new Object(), new Object()));
        when(platformAdminClient.listBoards("Bearer token")).thenReturn(List.of(new Object(), new Object()));
        when(platformAdminClient.listAuditLogs("Bearer token")).thenReturn(List.of(
                java.util.Map.of("actionType", "CREATE"),
                java.util.Map.of("actionType", "MOVE"),
                java.util.Map.of("actionType", "CREATE")
        ));

        ResponseEntity<AdminStatsResponse> response = controller.getStats("Bearer token", "PLATFORM_ADMIN");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalUsers()).isEqualTo(2);
        assertThat(response.getBody().getTotalWorkspaces()).isEqualTo(3);
        assertThat(response.getBody().getTotalBoards()).isEqualTo(2);
        assertThat(response.getBody().getTotalCards()).isEqualTo(2);
        assertThat(response.getBody().getActiveTeams()).isEqualTo(3);
        verify(adminGuard).requirePlatformAdmin("PLATFORM_ADMIN");
    }
}
