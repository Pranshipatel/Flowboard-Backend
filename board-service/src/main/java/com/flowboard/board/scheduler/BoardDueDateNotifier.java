package com.flowboard.board.scheduler;

import com.flowboard.board.entity.Board;
import com.flowboard.board.repository.BoardRepository;
import com.flowboard.board.client.WorkspaceClient;
import com.flowboard.board.dto.WorkspaceMemberResponse;
import com.flowboard.board.dto.SendNotificationRequest;
import com.flowboard.board.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoardDueDateNotifier {

    private final BoardRepository boardRepository;
    private final WorkspaceClient workspaceClient;
    private final RabbitTemplate rabbitTemplate;

    // Run every day at 00:00 (midnight) or for testing, you could run it more frequently.
    // For this demonstration, we'll run it every hour to check for due boards.
    @Scheduled(fixedRate = 3600000)
    public void checkDueDates() {
        log.info("Running scheduled check for board due dates...");

        LocalDateTime now = LocalDateTime.now();
        // Looking for boards whose due date is within the next 24 hours, or already passed
        // We might want to only check active boards.
        List<Board> boards = boardRepository.findAll();

        for (Board board : boards) {
            if (!board.isClosed() && board.getDueDate() != null && now.isAfter(board.getDueDate())) {
                notifyWorkspaceMembers(board);
            }
        }
    }

    private void notifyWorkspaceMembers(Board board) {
        try {
            // Using the board creator's ID to fetch workspace members since we need a valid userId for the Feign client
            List<WorkspaceMemberResponse> members = workspaceClient.getMembers(board.getWorkspaceId(), board.getCreatedById());
            
            for (WorkspaceMemberResponse member : members) {
                SendNotificationRequest notification = SendNotificationRequest.builder()
                        .recipientId(member.getUserId())
                        .actorId(board.getCreatedById())
                        .type("DUE_DATE")
                        .title("Task Overdue")
                        .message("The task '" + board.getName() + "' is overdue.")
                        .relatedId(board.getId())
                        .relatedType("BOARD")
                        .deepLinkUrl("/b/" + board.getId())
                        .build();

                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.NOTIFICATION_EXCHANGE,
                        RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                        notification
                );
            }
        } catch (Exception e) {
            log.error("Failed to process due date notifications for board {}", board.getId(), e);
        }
    }
}
