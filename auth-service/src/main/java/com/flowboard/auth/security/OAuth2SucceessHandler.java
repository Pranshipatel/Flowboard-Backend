package com.flowboard.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;

import java.io.IOException;

/**
 * OAuth2 Success Handler
 *
 * Handles successful OAuth login and generates JWT for the user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SucceessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${app.frontend-url:http://flowboard-frontend-pranshi.s3-website.ap-south-1.amazonaws.com}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // Extract OAuth2 user details
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // Fetch user from DB
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "OAuth user not found after login: " + email));

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getEmail(), user.getId(), user.getRole().name());

        log.info("OAuth2 login success: email={} userId={}", email, user.getId());

        // Redirect to frontend with token and user details
        String redirectUrl = frontendUrl + "/oauth2/callback?token=" + token
                + "&userId=" + user.getId()
                + "&role=" + user.getRole().name();

        response.sendRedirect(redirectUrl);
    }
}
