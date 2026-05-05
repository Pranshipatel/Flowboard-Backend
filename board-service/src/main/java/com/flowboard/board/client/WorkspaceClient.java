package com.flowboard.board.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.flowboard.board.dto.WorkspaceResponse;
import com.flowboard.board.dto.WorkspaceMemberResponse;
import java.util.List;

import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "WORKSPACE-SERVICE", path = "/api/v1/workspaces")
public interface WorkspaceClient {

    @GetMapping("/{id}")
    WorkspaceResponse getWorkspaceById(@PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/{id}")
    WorkspaceResponse getPublicWorkspaceById(@PathVariable("id") Long id);

    @GetMapping("/{id}/members")
    List<WorkspaceMemberResponse> getMembers(@PathVariable("id") Long id, @RequestHeader("X-User-Id") Long userId);
}
