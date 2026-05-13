package com.flowboard.auth.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.flowboard.auth.entity.ROLE;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * OAuth User Service
 *
 * Handles OAuth2 login and user provisioning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthUserService extends DefaultOAuth2UserService {

    private final UserRepository repository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // Load user from OAuth provider
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();

        String email = extractEmail(oAuth2User, provider);
        String name = extractName(oAuth2User, provider);

        // Validate email presence
        if (email == null || email.isBlank()) {
            log.warn("OAuth2 login from {} returned no email", provider);
            throw new OAuth2AuthenticationException("No Email returned from OAuth2 provider: " + provider);
        }

        Optional<User> existingUser = repository.findByEmail(email);

        // Create new user if not exists
        User user = existingUser.orElseGet(() -> {

            String baseUsername = email.split("@")[0];
            String uniqueUsername = makeUniqueUsername(baseUsername);
            String displayName = name != null ? name : baseUsername;

            User newUser = User.builder()
                    .fullName(displayName)
                    .email(email)
                    .username(uniqueUsername)
                    .password("") // OAuth users don't use password
                    .role(ROLE.MEMBER)
                    .provider(provider.toUpperCase())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();

            repository.save(newUser);
            log.info("New user created via OAuth2 provider={} email={}", provider, email);

            return newUser;
        });

        // Check account status
        if (!user.isActive()) {
            throw new OAuth2AuthenticationException("Account is deactivated");
        }

        // Update provider if missing
        if (user.getProvider() == null || user.getProvider().isBlank()) {
            user.setProvider(provider.toUpperCase());
            repository.save(user);
        }

        return oAuth2User;
    }

    // Extract email based on provider
    private String extractEmail(OAuth2User user, String provider) {
        if ("github".equals(provider)) {
            Object email = user.getAttribute("email");
            return email != null ? email.toString() : null;
        }
        return user.getAttribute("email");
    }

    // Extract name based on provider
    private String extractName(OAuth2User user, String provider) {
        if ("github".equals(provider)) {
            Object name = user.getAttribute("name");
            if (name != null) {
                return name.toString();
            }
            Object login = user.getAttribute("login");
            return login != null ? login.toString() : null;
        }
        return user.getAttribute("name");
    }

    // Ensure unique username
    private String makeUniqueUsername(String base) {
        String candidate = base;
        int suffix = 1;

        while (repository.existsByUsername(candidate)) {
            candidate = base + suffix++;
        }

        return candidate;
    }
}
