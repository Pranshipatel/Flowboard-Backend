package com.flowboard.auth.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OtpStoreTest {

    @Test
    void saveVerifyExistsAndDeleteWorkForActiveOtp() {
        OtpStore store = new OtpStore();

        store.save("user@example.com", "123456", 10);

        assertTrue(store.exists("user@example.com"));
        assertTrue(store.verify("user@example.com", "123456"));
        assertFalse(store.verify("user@example.com", "000000"));

        store.delete("user@example.com");
        assertFalse(store.exists("user@example.com"));
    }

    @Test
    void expiredOtpIsRemoved() {
        OtpStore store = new OtpStore();
        store.save("user@example.com", "123456", -1);

        assertFalse(store.exists("user@example.com"));
        assertFalse(store.verify("user@example.com", "123456"));
    }
}
