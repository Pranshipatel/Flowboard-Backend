package com.flowboard.list.service;



import java.util.List;

import com.flowboard.list.dto.CreateListRequest;
import com.flowboard.list.dto.ListResponse;
import com.flowboard.list.dto.MoveListRequest;
import com.flowboard.list.dto.ReorderListRequest;
import com.flowboard.list.dto.UpdateListRequest;

public interface ListService {

    // ================= CREATE =================
    ListResponse createList(CreateListRequest request, Long userId);


    // ================= READ =================
    ListResponse getListById(Long listId);

    List<ListResponse> getListsByBoard(Long boardId);

    List<ListResponse> getArchivedLists(Long boardId);


    // ================= UPDATE =================
    ListResponse updateList(Long listId, UpdateListRequest request, Long userId);

    List<ListResponse> reorderLists(ReorderListRequest request, Long userId);

    ListResponse moveList(Long listId, MoveListRequest request, Long userId);


    // ================= DELETE =================
    void deleteList(Long listId, Long userId);


    // ================= ARCHIVE =================
    ListResponse archiveList(Long listId, Long userId);

    ListResponse unarchiveList(Long listId, Long userId);
}