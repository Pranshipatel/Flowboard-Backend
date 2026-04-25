package com.flowboard.auth.entity;




import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * User Entity
 *
 * Represents application users in the system.
 * Implements UserDetails to integrate with Spring Security.
 *
 * Responsibilities:
 * - Store user credentials and profile data
 * - Provide authentication details to Spring Security
 * - Define account status (active, locked, expired, etc.)
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    /**
     * Primary Key (Auto-generated)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * Full name of the user
     */
    private String fullName;

    /**
     * Email (used for login)
     * Must be unique and not null
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Optional username (also unique)
     */
    @Column(unique = true)
    private String username;

    /**
     * Encrypted password
     */
    private String password;

    /**
     * Short bio or description
     */
    private String bio;

    /**
     * Role of the user (ENUM)
     */
    @Enumerated(EnumType.STRING)
    private ROLE role;

    /**
     * Indicates if account is active
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Indicates if email is verified
     */
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Profile avatar URL
     */
    private String avatarUrl;

    /**
     * Authentication provider (e.g., Google, GitHub)
     */
    private String provider;

    /**
     * Account creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Returns user authorities (roles/permissions)
     * Spring Security uses this for authorization
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return List.of(() -> role.name());
    }

    /**
     * Returns username for authentication
     * Here, email is used as the login identifier
     */
    @Override
    public String getUsername(){
        return email;
    }

    /**
     * Indicates whether the user is enabled
     */
    @Override
    public boolean isEnabled(){
        return active;
    }

    /**
     * Indicates whether account is expired
     * Always true → no expiration logic implemented
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether account is locked
     * Uses 'active' flag
     */
    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    /**
     * Indicates whether credentials are expired
     * Always true → no expiration logic implemented
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}