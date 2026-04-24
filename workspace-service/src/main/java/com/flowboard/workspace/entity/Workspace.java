package com.flowboard.workspace.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workspaces")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workspace {

    // Primary key
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Workspace name
    @Column(nullable = false)
    private String name;

    private String description;

    // Owner user ID
    @Column(nullable = false)
    private Long ownerId;

    // Visibility (default: PRIVATE)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    private String logoUrl;

    // Creation timestamp
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    // Workspace members
    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkspaceMember> members = new ArrayList<>();
}