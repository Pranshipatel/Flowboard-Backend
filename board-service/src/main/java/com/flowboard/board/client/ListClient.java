package com.flowboard.board.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;
import java.util.List;
import com.flowboard.board.dto.PublicListClientResponse;

@FeignClient(name = "list-service", path = "/api/v1/lists")
public interface ListClient {

    @GetMapping("/board/{boardId}")
    List<PublicListClientResponse> getByBoard(
            @PathVariable("boardId") Long boardId,
            @RequestHeader("X-Subscription-Plan") String plan,
            @RequestHeader("X-Subscription-Status") String status
    );

    @PostMapping
    Object createList(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-Subscription-Plan") String plan,
            @RequestHeader("X-Subscription-Status") String status
    );
}
