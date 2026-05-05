package com.admin.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminGuard {

    // Your auth-service endpoints require PLATFORM_ADMIN
    public void requirePlatformAdmin(String roleHeader) {
        if (roleHeader == null || !roleHeader.equalsIgnoreCase("PLATFORM_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Platform admin access required");
        }
    }
}