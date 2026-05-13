package com.flowboard.list.service;

import com.flowboard.list.dto.CreateListRequest;
import com.flowboard.list.dto.ListResponse;
import com.flowboard.list.dto.MoveListRequest;
import com.flowboard.list.dto.ReorderListRequest;
import com.flowboard.list.dto.UpdateListRequest;
import com.flowboard.list.entity.TaskList;
import com.flowboard.list.exception.CustomException;
import com.flowboard.list.repository.ListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListServiceImplTest {

    @Mock
    private ListRepository listRepository;

    @InjectMocks
    private ListServiceImpl listService;

    private TaskList testList;
    private CreateListRequest createRequest;

    @BeforeEach
    void setUp() {
        testList = TaskList.builder()
                .id(1L)
                .boardId(10L)
                .name("To Do")
                .position(0)
                .color("#FFFFFF")
                .isArchived(false)
                .build();

        createRequest = new CreateListRequest();
        createRequest.setBoardId(10L);
        createRequest.setName("To Do");
        createRequest.setColor("#FFFFFF");
    }

    @Test
    void createList_WhenValid_ShouldReturnResponse() {
        when(listRepository.findMaxPositionByBoardId(10L)).thenReturn(Optional.of(0));
        when(listRepository.save(any(TaskList.class))).thenAnswer(i -> {
            TaskList list = i.getArgument(0);
            list.setId(1L);
            return list;
        });

        ListResponse response = listService.createList(createRequest, 1L, true);

        assertNotNull(response);
        assertEquals("To Do", response.getName());
        assertEquals(1, response.getPosition());
        verify(listRepository).findMaxPositionByBoardId(10L);
        verify(listRepository).save(any(TaskList.class));
    }

    @Test
    void createList_WithRequestedPosition_ShouldShiftAndUsePosition() {
        createRequest.setPosition(2);
        when(listRepository.save(any(TaskList.class))).thenAnswer(i -> {
            TaskList list = i.getArgument(0);
            list.setId(2L);
            return list;
        });

        ListResponse response = listService.createList(createRequest, 1L, true);

        assertEquals(2, response.getPosition());
        verify(listRepository).shiftPositionsRight(10L, 2);
    }

    @Test
    void createList_WhenFreeLimitReached_ShouldThrow() {
        when(listRepository.countByBoardIdAndIsArchivedFalse(10L)).thenReturn(2L);

        assertThrows(CustomException.class, () -> listService.createList(createRequest, 1L, false));
    }

    @Test
    void getListById_WhenExists_ShouldReturnList() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        ListResponse response = listService.getListById(1L);

        assertNotNull(response);
        assertEquals("To Do", response.getName());
    }

    @Test
    void getListById_WhenNotFound_ShouldThrowException() {
        when(listRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(CustomException.class, () -> listService.getListById(1L));
    }

    @Test
    void boardAndArchivedQueries_ShouldReturnResponses() {
        when(listRepository.findByBoardIdAndIsArchivedFalseOrderByPosition(10L)).thenReturn(List.of(testList));
        when(listRepository.findByBoardIdAndIsArchivedTrue(10L)).thenReturn(List.of(testList));

        assertEquals(1, listService.getListsByBoard(10L).size());
        assertEquals(1, listService.getArchivedLists(10L).size());
    }

    @Test
    void updateList_WhenValid_ShouldUpdateNameAndOptionalColor() {
        UpdateListRequest request = new UpdateListRequest();
        request.setName("Doing");
        request.setColor("#000000");
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        ListResponse response = listService.updateList(1L, request, 1L);

        assertEquals("Doing", response.getName());
        assertEquals("#000000", response.getColor());
        verify(listRepository).save(testList);
    }

    @Test
    void updateList_WhenArchived_ShouldThrow() {
        testList.setArchived(true);
        UpdateListRequest request = new UpdateListRequest();
        request.setName("Doing");
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        assertThrows(CustomException.class, () -> listService.updateList(1L, request, 1L));
    }

    @Test
    void archiveList_WhenValid_ShouldArchive() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        ListResponse response = listService.archiveList(1L, 1L);

        assertTrue(response.isArchived());
        verify(listRepository).shiftPositionsLeft(10L, 0);
        verify(listRepository).save(testList);
    }

    @Test
    void archiveList_WhenAlreadyArchived_ShouldThrowException() {
        testList.setArchived(true);
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        assertThrows(CustomException.class, () -> listService.archiveList(1L, 1L));
    }

    @Test
    void deleteList_WhenValid_ShouldDelete() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        listService.deleteList(1L, 1L);

        verify(listRepository).shiftPositionsLeft(10L, 0);
        verify(listRepository).delete(testList);
    }

    @Test
    void deleteList_WhenArchived_ShouldNotShift() {
        testList.setArchived(true);
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        listService.deleteList(1L, 1L);

        verify(listRepository, never()).shiftPositionsLeft(anyLong(), anyInt());
        verify(listRepository).delete(testList);
    }

    @Test
    void reorderLists_WhenValid_ShouldPersistNewPositions() {
        TaskList second = TaskList.builder()
                .id(2L)
                .boardId(10L)
                .name("Done")
                .position(1)
                .color("#EEEEEE")
                .isArchived(false)
                .build();
        when(listRepository.findByBoardIdAndIsArchivedFalseOrderByPosition(10L))
                .thenReturn(List.of(testList, second), List.of(second, testList));
        ReorderListRequest request = new ReorderListRequest();
        request.setBoardId(10L);
        request.setOrderedListIds(List.of(2L, 1L));

        List<ListResponse> response = listService.reorderLists(request, 1L);

        assertEquals(2, response.size());
        assertEquals(0, second.getPosition());
        assertEquals(1, testList.getPosition());
        verify(listRepository, times(2)).save(any(TaskList.class));
    }

    @Test
    void reorderLists_WhenListDoesNotBelongToBoard_ShouldThrow() {
        when(listRepository.findByBoardIdAndIsArchivedFalseOrderByPosition(10L)).thenReturn(List.of(testList));
        ReorderListRequest request = new ReorderListRequest();
        request.setBoardId(10L);
        request.setOrderedListIds(List.of(99L));

        assertThrows(CustomException.class, () -> listService.reorderLists(request, 1L));
    }

    @Test
    void unarchiveList_WhenArchived_ShouldAppendToBoard() {
        testList.setArchived(true);
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));
        when(listRepository.findMaxPositionByBoardId(10L)).thenReturn(Optional.of(4));

        ListResponse response = listService.unarchiveList(1L, 1L);

        assertFalse(response.isArchived());
        assertEquals(5, response.getPosition());
        verify(listRepository).save(testList);
    }

    @Test
    void unarchiveList_WhenNotArchived_ShouldThrow() {
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        assertThrows(CustomException.class, () -> listService.unarchiveList(1L, 1L));
    }

    @Test
    void moveList_WithRequestedPosition_ShouldMoveAcrossBoards() {
        MoveListRequest request = new MoveListRequest();
        request.setTargetBoardId(20L);
        request.setTargetPosition(3);
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        ListResponse response = listService.moveList(1L, request, 1L);

        assertEquals(20L, response.getBoardId());
        assertEquals(3, response.getPosition());
        assertFalse(response.isArchived());
        verify(listRepository).shiftPositionsLeft(10L, 0);
        verify(listRepository).shiftPositionsRight(20L, 3);
        verify(listRepository).save(testList);
    }

    @Test
    void moveList_WithoutPosition_ShouldAppendAndHandleArchivedSource() {
        testList.setArchived(true);
        MoveListRequest request = new MoveListRequest();
        request.setTargetBoardId(20L);
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));
        when(listRepository.findMaxPositionByBoardId(20L)).thenReturn(Optional.empty());

        ListResponse response = listService.moveList(1L, request, 1L);

        assertEquals(0, response.getPosition());
        assertFalse(response.isArchived());
        verify(listRepository, never()).shiftPositionsLeft(anyLong(), anyInt());
    }

    @Test
    void moveList_WhenAlreadyOnTargetBoard_ShouldThrow() {
        MoveListRequest request = new MoveListRequest();
        request.setTargetBoardId(10L);
        when(listRepository.findById(1L)).thenReturn(Optional.of(testList));

        assertThrows(CustomException.class, () -> listService.moveList(1L, request, 1L));
    }
}
