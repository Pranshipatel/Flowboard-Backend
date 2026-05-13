package com.flowboard.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "01234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 60_000L);
    }

    @Test
    void generatedTokenCanBeReadAndValidated() {
        String token = jwtUtil.generateToken("user@example.com", 42L, "MEMBER");

        assertEquals("user@example.com", jwtUtil.extractEmail(token));
        assertEquals(42L, jwtUtil.extractUserId(token));
        assertEquals("MEMBER", jwtUtil.extractAllClaims(token).get("role"));
        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void invalidTokenIsRejected() {
        assertFalse(jwtUtil.isTokenValid("not-a-token"));
    }
}
