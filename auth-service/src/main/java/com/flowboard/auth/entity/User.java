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

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;


    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String username;

    private String password;

    private String bio;

    @Enumerated(EnumType.STRING)
    private ROLE role;

    @Builder.Default
    private boolean active = true;

    @Builder.Default
    private boolean emailVerified = false;

    private String avatarUrl;

    private String provider;

    private LocalDateTime createdAt;


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
        return username;
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
