package com.flowboard.board.scheduler;

import com.flowboard.board.client.WorkspaceClient;
import com.flowboard.board.dto.WorkspaceMemberResponse;
import com.flowboard.board.entity.Board;
import com.flowboard.board.entity.Visibility;
import com.flowboard.board.repository.BoardRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardDueDateNotifierTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private BoardDueDateNotifier notifier;

    @BeforeEach
    void setUp() {
        notifier = new BoardDueDateNotifier(boardRepository, workspaceClient, rabbitTemplate);
    }

    @Test
    void checkDueDates_WhenBoardIsOverdue_ShouldNotifyWorkspaceMembers() {
        Board overdue = board(1L, LocalDateTime.now().minusHours(1), false);
        when(boardRepository.findAll()).thenReturn(List.of(
                overdue,
                board(2L, LocalDateTime.now().plusHours(1), false),
                board(3L, LocalDateTime.now().minusHours(1), true),
                board(4L, null, false)
        ));
        when(workspaceClient.getMembers(10L, 1L)).thenReturn(List.of(
                WorkspaceMemberResponse.builder().userId(1L).build(),
                WorkspaceMemberResponse.builder().userId(2L).build()
        ));

        notifier.checkDueDates();

        verify(rabbitTemplate, times(2)).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void checkDueDates_WhenNotificationFails_ShouldContinueWithoutThrowing() {
        Board overdue = board(1L, LocalDateTime.now().minusHours(1), false);
        when(boardRepository.findAll()).thenReturn(List.of(overdue));
        doThrow(new RuntimeException("workspace unavailable")).when(workspaceClient).getMembers(10L, 1L);

        notifier.checkDueDates();

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    private static Board board(Long id, LocalDateTime dueDate, boolean closed) {
        return Board.builder()
                .id(id)
                .workspaceId(10L)
                .name("Board " + id)
                .visibility(Visibility.PUBLIC)
                .createdById(1L)
                .dueDate(dueDate)
                .isClosed(closed)
                .build();
    }
}
