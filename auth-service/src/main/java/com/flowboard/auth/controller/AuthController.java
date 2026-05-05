package com.flowboard.auth.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.flowboard.auth.dto.AuthResponse;
import com.flowboard.auth.dto.ChangePasswordRequest;
import com.flowboard.auth.dto.ForgotPasswordRequest;
import com.flowboard.auth.dto.LoginRequest;
import com.flowboard.auth.dto.RegisterRequest;
import com.flowboard.auth.dto.ResetPasswordRequest;
import com.flowboard.auth.dto.TokenRefreshRequest;
import com.flowboard.auth.dto.UpdateProfileRequest;
import com.flowboard.auth.dto.UserProfileDto;
import com.flowboard.auth.dto.VerifyOtpRequest;
import com.flowboard.auth.entity.ROLE;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.service.AuthService;

import java.util.List;

/**
 * Auth Controller
 *
 * Exposes REST APIs for:
 * - Authentication (register, login, logout)
 * - Token management
 * - Profile operations
 * - OTP flows (verification & password reset)
 * - Admin operations
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ================= AUTH =================

    // Register new user
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.ok(authService.register(request));
    }

    // Login user
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(authService.login(request));
    }

    // Logout user (stateless)
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader){
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok("Logged out successfully");
    }

    // Validate JWT token
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestParam String token){
        return ResponseEntity.ok(authService.validateToken(token));
    }

    // Refresh token
    @PostMapping("/refresh")
    public ResponseEntity<String> refreshToken(@Valid @RequestBody TokenRefreshRequest request){
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }


    // ================= PROFILE =================

    // Get logged-in user profile
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal User user){
        User found = authService.getUserById(user.getId());
        return ResponseEntity.ok(toProfileDto(found));
    }

    // Update profile
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@AuthenticationPrincipal User user,
                                                @Valid @RequestBody UpdateProfileRequest request){
        authService.updateProfile(user.getId(), request);
        return ResponseEntity.ok("Profile updated successfully");
    }

    // Change password
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal User user,
                                                 @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
        return ResponseEntity.ok("Password changed successfully");
    }


    // ================= SEARCH =================

    // Search users by keyword (returns safe DTOs, not raw entities)
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileDto>> searchUsers(@RequestParam String key){
        return ResponseEntity.ok(
            authService.searchUsers(key).stream()
                .map(this::toProfileDto)
                .collect(java.util.stream.Collectors.toList())
        );
    }

    // Get user by ID (public lookup for workspace member details)
    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable Long id){
        User user = authService.getUserById(id);
        return ResponseEntity.ok(toProfileDto(user));
    }


    // ================= ACCOUNT =================

    // Deactivate account
    @DeleteMapping("/deactivate")
    public ResponseEntity<String> deactivate(@AuthenticationPrincipal User user){
        authService.deactivateAccount(user.getId());
        return ResponseEntity.ok("Account deactivated");
    }


    // ================= ADMIN =================

    // Get all users (admin only)
    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<List<User>> getAllUsers(){
        return ResponseEntity.ok(authService.getAllUsers());
    }

    // Get users by role (admin only)
    @GetMapping("/admin/users/role/{role}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable ROLE role){
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }

    // Update user role (admin only)
    @PutMapping("/admin/users/{id}/role")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<String> updateUserRole(@PathVariable Long id, @RequestParam String role){
        authService.updateUserRole(id, role);
        return ResponseEntity.ok("User role updated successfully");
    }

    // Suspend user (admin only)
    @PutMapping("/admin/users/{id}/suspend")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<String> suspendUser(@PathVariable Long id){
        authService.suspendUser(id);
        return ResponseEntity.ok("User suspended");
    }

    // Reactivate user (admin only)
    @PutMapping("admin/users/{id}/reactivate")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<String> reactivateUser(@PathVariable Long id){
        authService.reactivateUser(id);
        return ResponseEntity.ok("User reactivated");
    }

    // Delete user permanently (admin only)
    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id){
        authService.deleteUser(id);
        return ResponseEntity.ok("User permanently deleted");
    }


    // ================= OTP / EMAIL =================

    // Resend verification OTP
    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerifiation(@RequestParam @Email @NotBlank String email){
        authService.sendVerificationOtp(email);
        return ResponseEntity.ok("Verification OTP send to "+email);
    }

    // Verify email using OTP
    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@Valid @RequestBody VerifyOtpRequest request){
        authService.verifyEmail(request.getEmail(), request.getOtp());
        return ResponseEntity.ok("Email verified successfully. You can log in.");
    }

    // Forgot password (send OTP)
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request){
        authService.sendForgotPasswordOtp(request.getEmail());
        return ResponseEntity.ok("Password reset OTP send to "+request.getEmail());
    }

    // Reset password using OTP
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully. You can now log in.");
    }


    // ================= HELPER =================

    // Convert User entity → UserProfileDto
    private UserProfileDto toProfileDto(User user){
        return new UserProfileDto(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}