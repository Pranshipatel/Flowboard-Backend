package com.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String username;
    private String bio;
    private String role;
    private boolean active;
    private boolean emailVerified;
    private String avatarUrl;
    private String provider;
    private LocalDateTime createdAt;
}