package com.flowboard.list.service;

import com.flowboard.list.dto.CreateListRequest;
import com.flowboard.list.dto.ListResponse;
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
}
