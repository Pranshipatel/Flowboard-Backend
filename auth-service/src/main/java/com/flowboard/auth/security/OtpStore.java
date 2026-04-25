package com.flowboard.auth.security;


import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OTP Store
 *
 * In-memory storage for OTPs with expiry handling.
 */
@Component
public class OtpStore {

    // Thread-safe storage for OTP entries
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    // Save OTP with expiry time
    public void save(String email, String otp, int expiryMinutes){
        store.put(email, new OtpEntry(otp, LocalDateTime.now().plusMinutes(expiryMinutes)));
    }

    // Verify OTP and check expiry
    public boolean verify(String email, String otp){
        OtpEntry entry = store.get(email);
        if(entry == null) return false;

        if(LocalDateTime.now().isAfter(entry.expiry())){
            store.remove(email);
            return false;
        }

        return entry.otp().equals(otp);
    }

    // Remove OTP after successful verification or expiry
    public void delete(String email){
        store.remove(email);
    }

    // Check if OTP exists and is still valid
    public boolean exists(String email){
        OtpEntry entry = store.get(email);
        if(entry == null) return false;

        if(LocalDateTime.now().isAfter(entry.expiry())){
            store.remove(email);
            return false;
        }

        return true;
    }

    // Internal record to store OTP and expiry time
    private record OtpEntry(String otp, LocalDateTime expiry){}
}