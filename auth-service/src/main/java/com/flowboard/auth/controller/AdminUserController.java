//package com.flowboard.auth.controller;
//
//import com.flowboard.auth.dto.AdminUserResponse;
//import com.flowboard.auth.entity.User;
//import com.flowboard.auth.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/auth/admin/users")
//@RequiredArgsConstructor
//public class AdminUserController {
//
//    private final UserRepository userRepository;
//
//    @GetMapping
//    @PreAuthorize("hasAuthority('ADMIN')")
//    public ResponseEntity<List<AdminUserResponse>> listUsers() {
//        List<AdminUserResponse> users = userRepository.findAll()
//                .stream()
//                .map(this::toAdminResponse)
//                .toList();
//        return ResponseEntity.ok(users);
//    }
//
//    @GetMapping("/{id}")
//    @PreAuthorize("hasAuthority('ADMIN')")
//    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long id) {
//        User user = userRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//        return ResponseEntity.ok(toAdminResponse(user));
//    }
//
//    @DeleteMapping("/{id}")
//    @PreAuthorize("hasAuthority('ADMIN')")
//    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
//        if (!userRepository.existsById(id)) {
//            return ResponseEntity.status(404).body("User not found");
//        }
//        userRepository.deleteById(id);
//        return ResponseEntity.ok("User deleted");
//    }
//
//    private AdminUserResponse toAdminResponse(User u) {
//        return AdminUserResponse.builder()
//                .id(u.getId())
//                .fullName(u.getFullName())
//                .email(u.getEmail())
//                .username(u.getUsername())
//                .bio(u.getBio())
//                .role(u.getRole())
//                .active(u.isActive())
//                .emailVerified(u.isEmailVerified())
//                .avatarUrl(u.getAvatarUrl())
//                .provider(u.getProvider())
//                .createdAt(u.getCreatedAt())
//                .build();
//    }
//}