package com.flowboard.workspace.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flowboard.workspace.entity.Visibility;
import com.flowboard.workspace.entity.Workspace;

import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    // Workspaces owned by user
    List<Workspace> findByOwnerId(Long ownerId);

    // Workspaces where user is a member
    @Query("Select w from Workspace w JOIN w.members m Where m.userId= :userId")
    List<Workspace> findByMemberUserId(@Param("userId") Long userId);

    // Public workspaces
    List<Workspace> findByVisibility(Visibility visibility);

    // Check duplicate name for same owner
    boolean existsByNameAndOwnerId(String name, Long ownerId);

    // Count workspaces owned by user
    long countByOwnerId(Long ownerId);
}