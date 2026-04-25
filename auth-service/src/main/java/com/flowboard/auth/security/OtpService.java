package com.flowboard.auth.security;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.flowboard.auth.exception.CustomException;
import com.flowboard.auth.service.EmailService;

import java.security.SecureRandom;

/**
 * OTP Service
 *
 * Handles OTP generation, sending, and verification.
 */
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpStore otpStore;
    private final EmailService emailService;

    @Value("${otp.expiry-minutes:10}")
    private int expiryMinutes;

    // Generate 6-digit OTP
    private String generateOtp(){
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    // Send OTP for email verification
    public void sendVerificationOtp(String email){
        String otp = generateOtp();
        otpStore.save(email, otp, expiryMinutes);
        emailService.sendVerificationOtp(email, otp);
    }

    // Send OTP for forgot password flow
    public void sendForgotPasswordOtp(String email){
        String otp = generateOtp();
        otpStore.save(email, otp, expiryMinutes);
        emailService.sendForgotPasswordOtp(email, otp);
    }

    // Verify OTP
    public void verifyOtp(String email, String otp){
        if(!otpStore.verify(email, otp)){
            throw new CustomException("Invalid or expire OTP", HttpStatus.BAD_REQUEST);
        }
        otpStore.delete(email);
    }

    // Check if OTP already exists
    public boolean hasActiveOtp(String email){
        return otpStore.exists(email);
    }
}