package com.flowboard.auth.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.flowboard.auth.security.JwtFilter;
import com.flowboard.auth.security.OAuth2SucceessHandler;
import com.flowboard.auth.service.OAuthUserService;

/**
 * Security Configuration
 *
 * Configures:
 * - JWT-based authentication (stateless)
 * - Public & protected endpoints
 * - OAuth2 login (conditional)
 * - Security filter chain
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    // JWT filter for validating tokens on each request
    private final JwtFilter jwtFilter;

    // Optional OAuth2 user service
    @Autowired(required = false)
    private OAuthUserService oAuthUserService;

    // Optional OAuth2 success handler
    @Autowired(required = false)
    private OAuth2SucceessHandler oAuth2SucceessHandler;

    // Determines if OAuth2 is enabled
    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    // Constructor injection for JWT filter
    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Main security filter chain configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // Disable CSRF for stateless APIs
                .csrf(csrf -> csrf.disable())

                // Use stateless session (JWT-based auth)
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/validate",
                                "/api/v1/auth/verify-email",        
                                "/api/v1/auth/resend-verification",
                                "/api/v1/auth/forgot-password",     
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/search",
                                "/api/v1/auth/users/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // Admin endpoints (require authentication)
                        .requestMatchers("/api/v1/auth/admin/**").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );

        /**
         * Enable OAuth2 login only if client configuration exists
         */
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2

                    // Custom OAuth user service
                    .userInfoEndpoint(ui -> ui.userService(oAuthUserService))

                    // Success handler after login
                    .successHandler(oAuth2SucceessHandler)
            );
        }

        // Add JWT filter before default authentication filter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Password encoder bean (BCrypt)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}