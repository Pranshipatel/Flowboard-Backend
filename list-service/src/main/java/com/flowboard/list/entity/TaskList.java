package com.flowboard.list.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_lists")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Associated board
    @Column(nullable = false)
    private Long boardId;

    // List name
    @Column(nullable = false)
    private String name;

    // Order within board
    @Column(nullable = false)
    private Integer position;

    private String color;

    @Builder.Default
    private boolean isArchived = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}