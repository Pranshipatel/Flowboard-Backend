package com.flowboard.auth.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowboard.auth.exception.CustomException;
import com.flowboard.auth.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpStore otpStore;

    @Mock
    private EmailService emailService;

    private OtpService service;

    @BeforeEach
    void setUp() {
        service = new OtpService(otpStore, emailService);
        ReflectionTestUtils.setField(service, "expiryMinutes", 10);
    }

    @Test
    void sendVerificationOtpStoresAndEmailsOtp() {
        service.sendVerificationOtp("user@example.com");

        verify(otpStore).save(eq("user@example.com"), anyString(), eq(10));
        verify(emailService).sendVerificationOtp(eq("user@example.com"), anyString());
    }

    @Test
    void sendForgotPasswordOtpStoresAndEmailsOtp() {
        service.sendForgotPasswordOtp("user@example.com");

        verify(otpStore).save(eq("user@example.com"), anyString(), eq(10));
        verify(emailService).sendForgotPasswordOtp(eq("user@example.com"), anyString());
    }

    @Test
    void verifyOtpDeletesSuccessfulOtpAndRejectsInvalidOtp() {
        when(otpStore.verify("user@example.com", "123456")).thenReturn(true);
        service.verifyOtp("user@example.com", "123456");
        verify(otpStore).delete("user@example.com");

        when(otpStore.verify("user@example.com", "000000")).thenReturn(false);
        assertThrows(CustomException.class, () -> service.verifyOtp("user@example.com", "000000"));
    }

    @Test
    void hasActiveOtpDelegatesToStore() {
        when(otpStore.exists("user@example.com")).thenReturn(true);
        service.hasActiveOtp("user@example.com");
        verify(otpStore).exists("user@example.com");
    }
}
