package com.flowboard.auth.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flowboard.auth.entity.ROLE;
import com.flowboard.auth.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * User Repository
 *
 * Extends JpaRepository to provide CRUD operations for User entity.
 *
 * Responsibilities:
 * - Perform database operations on User table
 * - Provide custom query methods for authentication and search
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     *
     * @param email user email
     * @return Optional<User>
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username
     *
     * @param username username
     * @return Optional<User>
     */
    Optional<User> findByUsername(String username);

    /**
     * Check if email already exists
     *
     * @param email user email
     * @return true if exists, else false
     */
    boolean existsByEmail(String email);

    /**
     * Check if username already exists
     *
     * @param username username
     * @return true if exists, else false
     */
    boolean existsByUsername(String username);

    /**
     * Get all users by role
     *
     * @param role user role
     * @return list of users with given role
     */
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
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :key, '%')) OR" +
            " LOWER(u.username) LIKE LOWER(CONCAT('%', :key, '%'))")
    List<User> searchByNameOrUsername(@Param("key") String key);

    /**
     * Delete user by ID
     *
     * @param id user ID
     */
    void deleteById(Long id);
}