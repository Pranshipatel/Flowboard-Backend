package com.flowboard.auth.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.flowboard.auth.dto.AuthResponse;
import com.flowboard.auth.dto.ChangePasswordRequest;
import com.flowboard.auth.dto.LoginRequest;
import com.flowboard.auth.dto.RegisterRequest;
import com.flowboard.auth.dto.ResetPasswordRequest;
import com.flowboard.auth.dto.UpdateProfileRequest;
import com.flowboard.auth.entity.ROLE;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.exception.CustomException;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.security.JwtUtil;
import com.flowboard.auth.security.OtpService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Auth Service Implementation
 *
 * Handles authentication, user management, OTP flows, and admin operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    // ================= REGISTER =================
    @Override
    public AuthResponse register(RegisterRequest request) {

        if (repository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already exists", HttpStatus.BAD_REQUEST);
        }

        if (repository.existsByUsername(request.getUsername())) {
            throw new CustomException("Username already taken", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(ROLE.MEMBER)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(user);

        // Send email verification OTP
        otpService.sendVerificationOtp(request.getEmail());

        return new AuthResponse(
                "Registration successful. Please check your email for the verification OTP.",
                null
        );
    }

    // ================= LOGIN =================
    @Override
    public AuthResponse login(LoginRequest request) {

        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("Invalid email", HttpStatus.NOT_FOUND));

        // Check account status
        if (!user.isActive()) {
            throw new CustomException("Account is deactivated", HttpStatus.FORBIDDEN);
        }

        // Check email verification
        if (!user.isEmailVerified()) {
            if (!otpService.hasActiveOtp(user.getEmail())) {
                otpService.sendVerificationOtp(user.getEmail());
            }
            throw new CustomException(
                    "Email not verified. A new OTP has been sent to your email.",
                    HttpStatus.FORBIDDEN
            );
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException("Invalid password", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(
                user.getEmail(), user.getId(), user.getRole().name());

        return new AuthResponse("Login successful", token);
    }

    // ================= EMAIL VERIFICATION =================
    @Override
    public void sendVerificationOtp(String email){

        User user = repository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if(user.isEmailVerified()){
            throw new CustomException("Email is already verified", HttpStatus.BAD_REQUEST);
        }

        otpService.sendVerificationOtp(email);
    }

    @Override
    public void verifyEmail(String email, String otp){

        User user = repository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if(user.isEmailVerified()){
            throw new CustomException("Email is already verified", HttpStatus.BAD_REQUEST);
        }

        otpService.verifyOtp(email, otp);

        user.setEmailVerified(true);
        repository.save(user);

        log.info("Email verified for userId={}", user.getId());
    }

    // ================= FORGOT PASSWORD =================
    @Override
    public void sendForgotPasswordOtp(String email){

        User user = repository.findByEmail(email)
                .orElseThrow(() -> new CustomException("No account is found for this email", HttpStatus.NOT_FOUND));

        if(!user.isActive()){
            throw new CustomException("Account is deacivated", HttpStatus.FORBIDDEN);
        }

        otpService.sendForgotPasswordOtp(email);
        log.info("Forgot password OTP send to {}", email);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request){

        User user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        otpService.verifyOtp(request.getEmail(), request.getOtp());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);

        log.info("Password reset successfully for userId={}", user.getId());
    }

    // ================= TOKEN =================
    @Override
    public String validateToken(String token) {
        try {
            String email = jwtUtil.extractEmail(token);
            return "Valid token for user: " + email;
        } catch (Exception e) {
            throw new CustomException("Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    public String refreshToken(String token) {

        if (!jwtUtil.isTokenValid(token)) {
            throw new CustomException("Token is invalid or expired", HttpStatus.UNAUTHORIZED);
        }

        try {
            String email = jwtUtil.extractEmail(token);

            User user = repository.findByEmail(email)
                    .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

            if (!user.isActive()) {
                throw new CustomException("Account is deactivated", HttpStatus.FORBIDDEN);
            }

            return jwtUtil.generateToken(email, user.getId(), user.getRole().name());

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Cannot refresh token", HttpStatus.UNAUTHORIZED);
        }
    }

    // ================= PROFILE =================
    @Override
    public User getUserByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public User getUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }

    @Override
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUserById(userId);

        user.setFullName(request.getFullname());
        user.setUsername(request.getUsername());

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        repository.save(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUserById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new CustomException("Old password is incorrect", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        repository.save(user);
    }

    // Stateless logout (no server-side session)
    @Override
    public void logout(String token) {
        log.info("User logged out. Token invalidation requires a Redis blacklist for full security.");
    }

    @Override
    public void deactivateAccount(Long id) {
        User user = getUserById(id);
        user.setActive(false);
        repository.save(user);
        log.info("Account deactivated for userId: {}", id);
    }

    // ================= SEARCH =================
    @Override
    public List<User> searchUsers(String key) {
        return repository.searchByNameOrUsername(key);
    }

    // ================= ADMIN =================
    @Override
    public List<User> getAllUsers(){
        return repository.findAll();
    }

    @Override
    public List<User> getUsersByRole(ROLE role){
        return repository.findAllByRole(role);
    }

    @Override
    public void suspendUser(Long id){
        User user = getUserById(id);

        if(!user.isActive()){
            throw new CustomException("User is already suspended", HttpStatus.BAD_REQUEST);
        }

        user.setActive(false);
        repository.save(user);

        log.info("User suspended by admin: userId={}", id);
    }

    @Override
    public void reactivateUser(Long id) {
        User user = getUserById(id);

        if (user.isActive()) {
            throw new CustomException("User is already active", HttpStatus.BAD_REQUEST);
        }

        user.setActive(true);
        repository.save(user);

        log.info("User reactivated by admin: userId={}", id);
    }

    // Permanent delete
    @Override
    public void deleteUser(Long id) {

        if (!repository.existsById(id)) {
            throw new CustomException("User not found", HttpStatus.NOT_FOUND);
        }

        repository.deleteById(id);

        log.info("User permanently deleted by admin: userId={}", id);
    }
}