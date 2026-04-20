package com.flowboard.auth.dto;

import lombok.Data;

@Data
public class RegisterResponseDto {

    private String userName;
    private String passwordHash;
    private String email;
    private String role;
}