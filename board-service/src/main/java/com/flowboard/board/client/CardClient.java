package com.flowboard.board.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import com.flowboard.board.dto.PublicCardClientResponse;

@FeignClient(name = "card-service", path = "/api/v1/cards")
public interface CardClient {

    @GetMapping("/board/{boardId}")
    List<PublicCardClientResponse> getByBoard(
            @PathVariable("boardId") Long boardId,
            @RequestHeader("X-Subscription-Plan") String plan,
            @RequestHeader("X-Subscription-Status") String status
    );

    @PostMapping("/stats")
    List<BoardStatsResponse> getBoardStats(@RequestBody List<Long> boardIds);

    record BoardStatsResponse(Long boardId, long totalCards, long doneCards) {}
}
