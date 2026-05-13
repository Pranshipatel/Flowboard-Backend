package com.flowboard.list.controller;

import com.flowboard.list.dto.CreateListRequest;
import com.flowboard.list.dto.ListResponse;
import com.flowboard.list.dto.MoveListRequest;
import com.flowboard.list.dto.ReorderListRequest;
import com.flowboard.list.dto.UpdateListRequest;
import com.flowboard.list.exception.CustomException;
import com.flowboard.list.service.ListService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListControllerTest {

    @Mock
    private ListService listService;

    private ListController controller;
    private ListResponse response;

    @BeforeEach
    void setUp() {
        controller = new ListController(listService);
        response = ListResponse.builder().id(1L).boardId(10L).name("To Do").build();
    }

    @Test
    void endpoints_ShouldDelegateToServiceAndReturnResponses() {
        CreateListRequest create = new CreateListRequest();
        UpdateListRequest update = new UpdateListRequest();
        ReorderListRequest reorder = new ReorderListRequest();
        MoveListRequest move = new MoveListRequest();
        List<ListResponse> lists = List.of(response, ListResponse.builder().id(2L).name("Done").build());

        when(listService.createList(create, 1L, true)).thenReturn(response);
        when(listService.getListById(1L)).thenReturn(response);
        when(listService.getListsByBoard(10L)).thenReturn(lists);
        when(listService.getArchivedLists(10L)).thenReturn(List.of(response));
        when(listService.updateList(1L, update, 1L)).thenReturn(response);
        when(listService.reorderLists(reorder, 1L)).thenReturn(List.of(response));
        when(listService.moveList(1L, move, 1L)).thenReturn(response);
        when(listService.archiveList(1L, 1L)).thenReturn(response);
        when(listService.unarchiveList(1L, 1L)).thenReturn(response);

        assertEquals(HttpStatus.CREATED, controller.create(create, 1L, "PREMIUM", "ACTIVE").getStatusCode());
        assertEquals(response, controller.getById(1L).getBody());
        assertEquals(2, controller.getByBoard(10L, "PREMIUM", "ACTIVE").getBody().size());
        assertEquals(2, controller.getByBoard(10L, "FREE", "EXPIRED").getBody().size());
        assertEquals(1, controller.getArchived(10L).getBody().size());
        assertEquals(response, controller.update(1L, update, 1L).getBody());
        assertEquals(1, controller.reorder(reorder, 1L).getBody().size());
        assertEquals(response, controller.move(1L, move, 1L).getBody());
        assertEquals("List deleted successfully.", controller.delete(1L, 1L).getBody());
        assertEquals(response, controller.archive(1L, 1L).getBody());
        assertEquals(response, controller.unarchive(1L, 1L).getBody());

        verify(listService).deleteList(1L, 1L);
    }

    @Test
    void endpoints_WhenUserHeaderMissing_ShouldThrowBadRequest() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> controller.delete(1L, null)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
