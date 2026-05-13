package com.flowboard.auth.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flowboard.auth.entity.ROLE;
import com.flowboard.auth.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRole(ROLE role);

    /**
     * Search users by full name or username (case-insensitive)
     *
     * Uses JPQL query with LIKE and LOWER for flexible matching
     *
     * @param key search keyword
     * @return list of matching users
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :key, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :key, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :key, '%'))")
    List<User> searchUsers(@Param("key") String key);

    void deleteById(Long id);
}