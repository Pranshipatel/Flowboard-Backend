package com.flowboard.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String EMAIL = "alex@example.com";
    private static final String RAW_PASSWORD = "secret123";
    private static final String ENCODED_PASSWORD = "encoded";

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(10L)
                .fullName("Alex Rivera")
                .email(EMAIL)
                .username("alex")
                .password(ENCODED_PASSWORD)
                .role(ROLE.MEMBER)
                .active(true)
                .emailVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerCreatesMemberAndSendsVerificationOtp() {
        RegisterRequest request = registerRequest();
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        var response = service.register(request);

        assertEquals("Registration successful. Please check your email for the verification OTP.", response.getMessage());
        verify(repository).save(any(User.class));
        verify(otpService).sendVerificationOtp(EMAIL);
    }

    @Test
    void registerRejectsDuplicateEmailAndUsername() {
        RegisterRequest request = registerRequest();
        when(repository.existsByEmail(EMAIL)).thenReturn(true);
        assertThrows(CustomException.class, () -> service.register(request));

        when(repository.existsByEmail(EMAIL)).thenReturn(false);
        when(repository.existsByUsername("alex")).thenReturn(true);
        assertThrows(CustomException.class, () -> service.register(request));
    }

    @Test
    void loginReturnsTokenForVerifiedActiveUser() {
        LoginRequest request = loginRequest();
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(jwtUtil.generateToken(EMAIL, 10L, ROLE.MEMBER.name())).thenReturn("jwt");

        var response = service.login(request);

        assertEquals("Login successful", response.getMessage());
        assertEquals("jwt", response.getToken());
    }

    @Test
    void loginRejectsInactiveUnverifiedAndBadPasswordUsers() {
        LoginRequest request = loginRequest();

        user.setActive(false);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        assertThrows(CustomException.class, () -> service.login(request));

        user.setActive(true);
        user.setEmailVerified(false);
        when(otpService.hasActiveOtp(EMAIL)).thenReturn(false);
        assertThrows(CustomException.class, () -> service.login(request));
        verify(otpService).sendVerificationOtp(EMAIL);

        user.setEmailVerified(true);
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(false);
        assertThrows(CustomException.class, () -> service.login(request));
    }

    @Test
    void verificationAndPasswordResetFlowsUpdateUser() {
        user.setEmailVerified(false);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.sendVerificationOtp(EMAIL);
        verify(otpService).sendVerificationOtp(EMAIL);

        service.verifyEmail(EMAIL, "123456");
        assertTrue(user.isEmailVerified());
        verify(otpService).verifyOtp(EMAIL, "123456");
        verify(repository).save(user);

        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setEmail(EMAIL);
        reset.setOtp("654321");
        reset.setNewPassword("newpass");
        when(passwordEncoder.encode("newpass")).thenReturn("new-encoded");
        service.resetPassword(reset);
        assertEquals("new-encoded", user.getPassword());
    }

    @Test
    void verificationRejectsAlreadyVerifiedUser() {
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThrows(CustomException.class, () -> service.sendVerificationOtp(EMAIL));
        assertThrows(CustomException.class, () -> service.verifyEmail(EMAIL, "123456"));
    }

    @Test
    void forgotPasswordRejectsInactiveUserAndSendsOtpForActiveUser() {
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.sendForgotPasswordOtp(EMAIL);
        verify(otpService).sendForgotPasswordOtp(EMAIL);

        user.setActive(false);
        assertThrows(CustomException.class, () -> service.sendForgotPasswordOtp(EMAIL));
    }

    @Test
    void tokenValidationAndRefreshUseJwtUtility() {
        when(jwtUtil.extractEmail("token")).thenReturn(EMAIL);
        assertEquals("Valid token for user: " + EMAIL, service.validateToken("token"));

        when(jwtUtil.isTokenValid("token")).thenReturn(true);
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(EMAIL, 10L, ROLE.MEMBER.name())).thenReturn("new-token");
        assertEquals("new-token", service.refreshToken("token"));

        when(jwtUtil.isTokenValid("bad")).thenReturn(false);
        assertThrows(CustomException.class, () -> service.refreshToken("bad"));
    }

    @Test
    void profileOperationsReadAndUpdateUsers() {
        when(repository.findById(10L)).thenReturn(Optional.of(user));
        assertEquals(user, service.getUserById(10L));

        UpdateProfileRequest update = new UpdateProfileRequest();
        update.setFullname("Alex New");
        update.setUsername("alexnew");
        update.setAvatarUrl("avatar.png");
        update.setBio("bio");
        service.updateProfile(10L, update);
        assertEquals("Alex New", user.getFullName());
        assertEquals("alexnew", user.getUsername());
        assertEquals("avatar.png", user.getAvatarUrl());
        assertEquals("bio", user.getBio());

        ChangePasswordRequest change = new ChangePasswordRequest();
        change.setOldPassword(RAW_PASSWORD);
        change.setNewPassword("newpass");
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("changed");
        service.changePassword(10L, change);
        assertEquals("changed", user.getPassword());
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        when(repository.findById(10L)).thenReturn(Optional.of(user));
        ChangePasswordRequest change = new ChangePasswordRequest();
        change.setOldPassword("wrong");
        change.setNewPassword("newpass");
        when(passwordEncoder.matches("wrong", ENCODED_PASSWORD)).thenReturn(false);

        assertThrows(CustomException.class, () -> service.changePassword(10L, change));
    }

    @Test
    void adminOperationsDelegateAndValidateState() {
        when(repository.findById(10L)).thenReturn(Optional.of(user));
        when(repository.findAll()).thenReturn(List.of(user));
        when(repository.findAllByRole(ROLE.MEMBER)).thenReturn(List.of(user));
        when(repository.searchUsers("alex")).thenReturn(List.of(user));

        assertEquals(List.of(user), service.getAllUsers());
        assertEquals(List.of(user), service.getUsersByRole(ROLE.MEMBER));
        assertEquals(List.of(user), service.searchUsers("alex"));

        service.updateUserRole(10L, "platform_admin");
        assertEquals(ROLE.PLATFORM_ADMIN, user.getRole());

        service.suspendUser(10L);
        assertFalse(user.isActive());
        assertThrows(CustomException.class, () -> service.suspendUser(10L));

        service.reactivateUser(10L);
        assertTrue(user.isActive());
        assertThrows(CustomException.class, () -> service.reactivateUser(10L));

        service.deactivateAccount(10L);
        assertFalse(user.isActive());
    }

    @Test
    void invalidAdminAndLookupOperationsThrowCustomExceptions() {
        when(repository.findById(10L)).thenReturn(Optional.of(user));
        assertThrows(CustomException.class, () -> service.updateUserRole(10L, "not-a-role"));

        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> service.getUserById(99L));

        when(repository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        assertThrows(CustomException.class, () -> service.getUserByEmail("missing@example.com"));
    }

    @Test
    void deleteUserChecksExistenceBeforeDeleting() {
        when(repository.existsById(10L)).thenReturn(true);
        service.deleteUser(10L);
        verify(repository).deleteById(10L);

        when(repository.existsById(11L)).thenReturn(false);
        assertThrows(CustomException.class, () -> service.deleteUser(11L));
        verify(repository, never()).deleteById(11L);
    }

    @Test
    void logoutDoesNotRequirePersistence() {
        service.logout("Bearer token");
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Alex Rivera");
        request.setEmail(EMAIL);
        request.setUsername("alex");
        request.setPassword(RAW_PASSWORD);
        return request;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail(EMAIL);
        request.setPassword(RAW_PASSWORD);
        return request;
    }
}
