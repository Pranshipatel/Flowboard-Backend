package com.flowboard.board.service;

import com.flowboard.board.dto.BoardResponse;
import com.flowboard.board.dto.CreateBoardRequest;
import com.flowboard.board.dto.WorkspaceResponse;
import com.flowboard.board.entity.Board;
import com.flowboard.board.entity.BoardMember;
import com.flowboard.board.entity.BoardMemberRole;
import com.flowboard.board.entity.Visibility;
import com.flowboard.board.exception.CustomException;
import com.flowboard.board.client.CardClient;
import com.flowboard.board.client.ListClient;
import com.flowboard.board.client.WorkspaceClient;
import com.flowboard.board.repository.BoardMemberRepository;
import com.flowboard.board.repository.BoardRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardMemberRepository memberRepository;

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private CardClient cardClient;

    @Mock
    private ListClient listClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private BoardServiceImpl boardService;

    private Board testBoard;
    private CreateBoardRequest createRequest;
    private BoardMember adminMember;

    @BeforeEach
    void setUp() {
        testBoard = Board.builder()
                .id(1L)
                .workspaceId(10L)
                .name("Project Alpha")
                .description("Top Secret")
                .background("blue")
                .visibility(Visibility.PRIVATE)
                .createdById(1L)
                .isClosed(false)
                .build();

        createRequest = new CreateBoardRequest();
        createRequest.setWorkspaceId(10L);
        createRequest.setName("Project Alpha");

        adminMember = BoardMember.builder()
                .board(testBoard)
                .userId(1L)
                .role(BoardMemberRole.ADMIN)
                .build();
    }

    @Test
    void createBoard_WhenValid_ShouldReturnBoard() {
        when(workspaceClient.getWorkspaceById(10L, 1L)).thenReturn(
                WorkspaceResponse.builder().id(10L).ownerId(1L).name("Workspace").build()
        );
        when(boardRepository.save(any(Board.class))).thenAnswer(i -> {
            Board b = i.getArgument(0);
            b.setId(1L);
            return b;
        });

        BoardResponse response = boardService.createBoard(createRequest, 1L, true);

        assertNotNull(response);
        assertEquals("Project Alpha", response.getName());
        verify(boardRepository).save(any(Board.class));
        verify(memberRepository).save(any(BoardMember.class));
    }

    @Test
    void getBoardById_WhenExists_ShouldReturnBoard() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));

        BoardResponse response = boardService.getBoardById(1L, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Project Alpha", response.getName());
    }

    @Test
    void closeBoard_WhenAdmin_ShouldClose() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepository.findByBoardIdAndUserId(1L, 1L)).thenReturn(Optional.of(adminMember));

        BoardResponse response = boardService.closeBoard(1L, 1L);

        assertTrue(response.isClosed());
        verify(boardRepository).save(testBoard);
    }

    @Test
    void closeBoard_WhenNotAdmin_ShouldThrowException() {
        BoardMember observerMember = BoardMember.builder().role(BoardMemberRole.OBSERVER).build();
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));
        when(memberRepository.findByBoardIdAndUserId(1L, 2L)).thenReturn(Optional.of(observerMember));

        assertThrows(CustomException.class, () -> boardService.closeBoard(1L, 2L));
    }

    @Test
    void deleteBoard_WhenCreator_ShouldDelete() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));

        boardService.deleteBoard(1L, 1L);

        verify(boardRepository).delete(testBoard);
    }

    @Test
    void deleteBoard_WhenNotCreator_ShouldThrowException() {
        when(boardRepository.findById(1L)).thenReturn(Optional.of(testBoard));

        assertThrows(CustomException.class, () -> boardService.deleteBoard(1L, 2L));
    }
}
