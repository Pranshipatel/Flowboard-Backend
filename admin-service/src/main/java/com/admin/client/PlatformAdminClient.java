package com.admin.client;

import com.admin.dto.BroadcastRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PlatformAdminClient {

    private final WebClient.Builder webClientBuilder;
    private final String workspaceBaseUrl;
    private final String boardBaseUrl;
    private final String cardBaseUrl;
    private final String notificationBaseUrl;

    public PlatformAdminClient(
            WebClient.Builder webClientBuilder,
            @Value("${workspace.service.base-url:http://WORKSPACE-SERVICE}") String workspaceBaseUrl,
            @Value("${board.service.base-url:http://BOARD-SERVICE}") String boardBaseUrl,
            @Value("${card.service.base-url:http://CARD-SERVICE}") String cardBaseUrl,
            @Value("${notification.service.base-url:http://NOTIFICATION-SERVICE}") String notificationBaseUrl
    ) {
        this.webClientBuilder = webClientBuilder;
        this.workspaceBaseUrl = workspaceBaseUrl;
        this.boardBaseUrl = boardBaseUrl;
        this.cardBaseUrl = cardBaseUrl;
        this.notificationBaseUrl = notificationBaseUrl;
    }

    public List<Object> listWorkspaces(String authorization) {
        return getList(workspaceBaseUrl + "/api/v1/workspaces/admin", authorization);
    }

    public String deleteWorkspace(Long id, String authorization) {
        return webClientBuilder.build().delete()
                .uri(workspaceBaseUrl + "/api/v1/workspaces/admin/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("X-User-Role", "PLATFORM_ADMIN")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public List<Object> listBoards(String authorization) {
        return getList(boardBaseUrl + "/api/v1/boards/admin", authorization);
    }

    public Object closeBoard(Long id, String authorization) {
        return boardMutation(id, "close", authorization);
    }

    public Object reopenBoard(Long id, String authorization) {
        return boardMutation(id, "reopen", authorization);
    }

    public String deleteBoard(Long id, String authorization) {
        return webClientBuilder.build().delete()
                .uri(boardBaseUrl + "/api/v1/boards/admin/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("X-User-Role", "PLATFORM_ADMIN")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public List<Object> listAuditLogs(String authorization) {
        return getList(cardBaseUrl + "/api/v1/cards/admin/audit-logs", authorization);
    }

    public List<Object> listOverdueCards(String authorization) {
        return getList(cardBaseUrl + "/api/v1/cards/admin/overdue", authorization);
    }

    public List<Object> sendBroadcast(BroadcastRequest request, String authorization, Long actorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipientIds", request.getRecipientIds());
        payload.put("actorId", actorId);
        payload.put("type", "BROADCAST");
        payload.put("title", request.getTitle());
        payload.put("message", request.getMessage());
        payload.put("relatedType", "PLATFORM");

        return webClientBuilder.build().post()
                .uri(notificationBaseUrl + "/api/v1/notifications/send/bulk")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("X-User-Role", "PLATFORM_ADMIN")
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(Object.class)
                .collectList()
                .block();
    }

    private List<Object> getList(String uri, String authorization) {
        return webClientBuilder.build().get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("X-User-Role", "PLATFORM_ADMIN")
                .retrieve()
                .bodyToFlux(Object.class)
                .collectList()
                .block();
    }

    private Object boardMutation(Long id, String action, String authorization) {
        return webClientBuilder.build().put()
                .uri(boardBaseUrl + "/api/v1/boards/admin/{id}/" + action, id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .header("X-User-Role", "PLATFORM_ADMIN")
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }
}
