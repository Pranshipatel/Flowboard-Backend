package com.flowboard.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.flowboard.auth.dto.AuthResponse;
import com.flowboard.auth.dto.ChangePasswordRequest;
import com.flowboard.auth.dto.ForgotPasswordRequest;
import com.flowboard.auth.dto.LoginRequest;
import com.flowboard.auth.dto.RegisterRequest;
import com.flowboard.auth.dto.ResetPasswordRequest;
import com.flowboard.auth.dto.TokenRefreshRequest;
import com.flowboard.auth.dto.UpdateProfileRequest;
import com.flowboard.auth.dto.VerifyOtpRequest;
import com.flowboard.auth.entity.ROLE;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.service.AuthService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
        user = User.builder()
                .id(7L)
                .fullName("Nina Patel")
                .email("nina@example.com")
                .username("nina")
                .avatarUrl("avatar.png")
                .role(ROLE.MEMBER)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void authEndpointsDelegateToService() {
        RegisterRequest register = new RegisterRequest();
        LoginRequest login = new LoginRequest();
        when(authService.register(register)).thenReturn(new AuthResponse("registered", null));
        when(authService.login(login)).thenReturn(new AuthResponse("logged-in", "token"));

        assertEquals("registered", controller.register(register).getBody().getMessage());
        assertEquals("token", controller.login(login).getBody().getToken());

        assertEquals("Logged out successfully", controller.logout("Bearer abc").getBody());
        verify(authService).logout("abc");
    }

    @Test
    void tokenEndpointsDelegateToService() {
        TokenRefreshRequest request = new TokenRefreshRequest();
        request.setRefreshToken("old");
        when(authService.validateToken("token")).thenReturn("valid");
        when(authService.refreshToken("old")).thenReturn("new");

        assertEquals("valid", controller.validateToken("token").getBody());
        assertEquals("new", controller.refreshToken(request).getBody());
    }

    @Test
    void profileEndpointsMapUsersToDtos() {
        when(authService.getUserById(7L)).thenReturn(user);

        var profile = controller.getProfile(user).getBody();
        assertEquals("nina", profile.getUsername());
        assertEquals("nina@example.com", profile.getEmail());

        UpdateProfileRequest update = new UpdateProfileRequest();
        assertEquals("Profile updated successfully", controller.updateProfile(user, update).getBody());
        verify(authService).updateProfile(7L, update);

        ChangePasswordRequest change = new ChangePasswordRequest();
        assertEquals("Password changed successfully", controller.changePassword(user, change).getBody());
        verify(authService).changePassword(7L, change);
    }

    @Test
    void searchAndLookupEndpointsReturnSafeDtos() {
        when(authService.searchUsers("nin")).thenReturn(List.of(user));
        when(authService.getUserById(7L)).thenReturn(user);

        assertEquals(1, controller.searchUsers("nin").getBody().size());
        assertEquals("Nina Patel", controller.getUserById(7L).getBody().getFullName());
    }

    @Test
    void accountAndAdminEndpointsDelegateToService() {
        when(authService.getAllUsers()).thenReturn(List.of(user));
        when(authService.getUsersByRole(ROLE.MEMBER)).thenReturn(List.of(user));

        assertEquals("Account deactivated", controller.deactivate(user).getBody());
        assertEquals(1, controller.getAllUsers().getBody().size());
        assertEquals(1, controller.getUsersByRole(ROLE.MEMBER).getBody().size());
        assertEquals("User role updated successfully", controller.updateUserRole(7L, "PLATFORM_ADMIN").getBody());
        assertEquals("User suspended", controller.suspendUser(7L).getBody());
        assertEquals("User reactivated", controller.reactivateUser(7L).getBody());
        assertEquals("User permanently deleted", controller.deleteUser(7L).getBody());
    }

    @Test
    void otpAndPasswordEndpointsDelegateToService() {
        VerifyOtpRequest verifyOtp = new VerifyOtpRequest();
        verifyOtp.setEmail("nina@example.com");
        verifyOtp.setOtp("123456");
        ForgotPasswordRequest forgot = new ForgotPasswordRequest();
        forgot.setEmail("nina@example.com");
        ResetPasswordRequest reset = new ResetPasswordRequest();

        assertEquals("Verification OTP send to nina@example.com",
                controller.resendVerifiation("nina@example.com").getBody());
        assertEquals("Email verified successfully. You can log in.",
                controller.verifyEmail(verifyOtp).getBody());
        assertEquals("Password reset OTP send to nina@example.com",
                controller.forgotPassword(forgot).getBody());
        assertEquals("Password reset successfully. You can now log in.",
                controller.resetPassword(reset).getBody());
    }
}
