package com.admin.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDtoTest {

    @Test
    void adminStatsResponseStoresAllValuesFromConstructorAndSetters() {
        AdminStatsResponse stats = new AdminStatsResponse(10, 3, 8, 6);

        assertThat(stats.getTotalUsers()).isEqualTo(10);
        assertThat(stats.getTotalWorkspaces()).isEqualTo(3);
        assertThat(stats.getTotalBoards()).isEqualTo(8);
        assertThat(stats.getActiveUsersToday()).isEqualTo(6);

        stats.setTotalUsers(11);
        stats.setTotalWorkspaces(4);
        stats.setTotalBoards(9);
        stats.setActiveUsersToday(7);

        assertThat(stats.getTotalUsers()).isEqualTo(11);
        assertThat(stats.getTotalWorkspaces()).isEqualTo(4);
        assertThat(stats.getTotalBoards()).isEqualTo(9);
        assertThat(stats.getActiveUsersToday()).isEqualTo(7);
    }

    @Test
    void adminStatsResponseSupportsNoArgsConstruction() {
        AdminStatsResponse stats = new AdminStatsResponse();

        assertThat(stats.getTotalUsers()).isZero();
        assertThat(stats.getTotalWorkspaces()).isZero();
        assertThat(stats.getTotalBoards()).isZero();
        assertThat(stats.getActiveUsersToday()).isZero();
    }

    @Test
    void adminUserResponseStoresAllProfileFields() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 9, 12, 45);
        AdminUserResponse user = new AdminUserResponse();

        user.setId(42L);
        user.setFullName("Ada Lovelace");
        user.setEmail("ada@example.com");
        user.setUsername("ada");
        user.setBio("First programmer");
        user.setRole("PLATFORM_ADMIN");
        user.setActive(true);
        user.setEmailVerified(true);
        user.setAvatarUrl("https://example.com/ada.png");
        user.setProvider("LOCAL");
        user.setCreatedAt(createdAt);

        assertThat(user.getId()).isEqualTo(42L);
        assertThat(user.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(user.getEmail()).isEqualTo("ada@example.com");
        assertThat(user.getUsername()).isEqualTo("ada");
        assertThat(user.getBio()).isEqualTo("First programmer");
        assertThat(user.getRole()).isEqualTo("PLATFORM_ADMIN");
        assertThat(user.isActive()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/ada.png");
        assertThat(user.getProvider()).isEqualTo("LOCAL");
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void adminUserResponseDefaultsBooleansToFalse() {
        AdminUserResponse user = new AdminUserResponse();

        assertThat(user.isActive()).isFalse();
        assertThat(user.isEmailVerified()).isFalse();
    }
}
