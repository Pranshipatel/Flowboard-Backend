package com.flowboard.auth.service;




import java.util.List;

import com.flowboard.auth.dto.AuthResponse;
import com.flowboard.auth.dto.ChangePasswordRequest;
import com.flowboard.auth.dto.LoginRequest;
import com.flowboard.auth.dto.RegisterRequest;
import com.flowboard.auth.dto.ResetPasswordRequest;
import com.flowboard.auth.dto.UpdateProfileRequest;
import com.flowboard.auth.entity.ROLE;
import com.flowboard.auth.entity.User;



public interface AuthService {

    // ================= AUTH =================

    // Register new user
	AuthResponse register(RegisterRequest request);


    AuthResponse login(LoginRequest request);

    // Validate JWT token
    String validateToken(String token);

    // Refresh JWT token
    String refreshToken(String token);

    // Logout user
    void logout(String token);



    void sendVerificationOtp(String email);

    // Verify email using OTP
    void verifyEmail(String email, String otp);


    // ========== FORGOT PASSWORD ==========

    // Send OTP for password reset
    void sendForgotPasswordOtp(String email);

    // Reset password using OTP
    void resetPassword(ResetPasswordRequest request);


    // ================= PROFILE =================

    // Get user by email
    User getUserByEmail(String email);

    // Get user by ID
    User getUserById(Long id);

    // Update user profile
    void updateProfile(Long userId, UpdateProfileRequest request);

    // Change user password
    void changePassword(Long userId, ChangePasswordRequest request);

    // Deactivate user account
    void deactivateAccount(Long id);


    // ================= SEARCH =================

    // Search users by keyword
    List<User> searchUsers(String key);


    // ========== ADMIN OPERATIONS ==========

    // Get all users
    List<User> getAllUsers();

    // Get users by role
    List<User> getUsersByRole(ROLE role);

    // Update user role
    void updateUserRole(Long userId, String role);

    // Suspend user account
    void suspendUser(Long id);

    // Reactivate suspended user
    void reactivateUser(Long id);

    // Delete user
    void deleteUser(Long id);
}