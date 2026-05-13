package com.admin.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.admin.dto.AdminUserResponse;

import java.util.List;

@Component
public class AuthAdminClient {

    private final WebClient.Builder webClientBuilder;
    private final String authBaseUrl;

    public AuthAdminClient(
            WebClient.Builder webClientBuilder,
            @Value("${auth.service.base-url:http://AUTH-SERVICE}") String authBaseUrl
    ) {
        this.webClientBuilder = webClientBuilder;
        this.authBaseUrl = authBaseUrl;
    }

    public List<AdminUserResponse> listUsers(String authorization) {
        return webClientBuilder.build()
                .get()
                .uri(authBaseUrl + "/api/v1/auth/admin/users")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToFlux(AdminUserResponse.class)
                .collectList()
                .block();
    }

    public AdminUserResponse getUser(Long id, String authorization) {
        return webClientBuilder.build()
                .get()
                .uri(authBaseUrl + "/api/v1/auth/admin/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(AdminUserResponse.class)
                .block();
    }

    public String deleteUser(Long id, String authorization) {
        return webClientBuilder.build()
                .delete()
                .uri(authBaseUrl + "/api/v1/auth/admin/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String updateUserRole(Long id, String role, String authorization) {
        return webClientBuilder.build()
                .put()
                .uri(authBaseUrl + "/api/v1/auth/admin/users/{id}/role?role={role}", id, role)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String suspendUser(Long id, String authorization) {
        return webClientBuilder.build()
                .put()
                .uri(authBaseUrl + "/api/v1/auth/admin/users/{id}/suspend", id)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String reactivateUser(Long id, String authorizationHeader) {
        return webClientBuilder.build()
                .put()
                .uri(authBaseUrl + "/api/v1/auth/admin/users/{id}/reactivate", id)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
