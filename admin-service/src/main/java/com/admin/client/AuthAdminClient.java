package com.admin.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.admin.dto.AdminUserResponse;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthAdminClient {

    private final WebClient.Builder webClientBuilder;

    private static final String AUTH_BASE = "http://AUTH-SERVICE";

    public List<AdminUserResponse> listUsers(String authorization) {
        return webClientBuilder.build()
                .get()
                .uri(AUTH_BASE + "/api/v1/auth/admin/users")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToFlux(AdminUserResponse.class)
                .collectList()
                .block();
    }

    public AdminUserResponse getUser(Long id, String authorization) {
        return webClientBuilder.build()
                .get()
                .uri(AUTH_BASE + "/api/v1/auth/admin/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(AdminUserResponse.class)
                .block();
    }

    public String deleteUser(Long id, String authorization) {
        return webClientBuilder.build()
                .delete()
                .uri(AUTH_BASE + "/api/v1/auth/admin/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String updateUserRole(Long id, String role, String authorization) {
        return webClientBuilder.build()
                .put()
                .uri(uriBuilder -> uriBuilder
                        .scheme("http")
                        .host("AUTH-SERVICE")
                        .path("/api/v1/auth/admin/users/{id}/role")
                        .queryParam("role", role)
                        .build(id))
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String suspendUser(Long id, String authorization) {
        return webClientBuilder.build()
                .put()
                .uri(AUTH_BASE + "/api/v1/auth/admin/users/{id}/suspend", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String reactivateUser(Long id, String authorizationHeader) {
        return webClientBuilder.build()
                .put()
                .uri(AUTH_BASE + "/api/v1/auth/admin/users/{id}/reactivate", id)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}