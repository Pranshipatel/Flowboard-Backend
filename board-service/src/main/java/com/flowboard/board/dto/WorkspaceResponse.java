package com.flowboard.board.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponse {
    private Long id;
    private String name;
    private Long ownerId;
    private String visibility;
}
