package com.flowboard.board.dto;

import lombok.Data;

@Data
public class PublicListClientResponse {
    private Long id;
    private Long boardId;
    private String name;
    private Integer position;
    private String color;
}
