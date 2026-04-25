package com.flowboard.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

import com.flowboard.auth.entity.ROLE;

@Data
@AllArgsConstructor
public class UserProfileDto {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String avatarUrl;
    private ROLE role;
    private boolean isActive;
    private LocalDateTime createdAt;
}
