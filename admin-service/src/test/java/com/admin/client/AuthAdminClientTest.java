package com.admin.client;

import com.admin.dto.AdminUserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthAdminClientTest {

    private static final String BASE_URL = "http://auth-service.test";

    private final List<ClientRequest> requests = new ArrayList<>();

    @Test
    void listUsersCallsAuthAdminEndpoint() {
        AuthAdminClient client = clientReturning("""
                [{"id":7,"email":"admin@example.com","active":true}]
                """);

        List<AdminUserResponse> users = client.listUsers("Bearer token");

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getId()).isEqualTo(7L);
        assertThat(users.get(0).getEmail()).isEqualTo("admin@example.com");
        assertThat(users.get(0).isActive()).isTrue();
        assertLastRequest("GET", "/api/v1/auth/admin/users", null);
    }

    @Test
    void getUserCallsAuthAdminEndpoint() {
        AuthAdminClient client = clientReturning("""
                {"id":7,"fullName":"Ada Lovelace"}
                """);

        AdminUserResponse user = client.getUser(7L, "Bearer token");

        assertThat(user.getFullName()).isEqualTo("Ada Lovelace");
        assertLastRequest("GET", "/api/v1/auth/admin/users/7", null);
    }

    @Test
    void deleteUserCallsAuthAdminEndpoint() {
        AuthAdminClient client = clientReturning("deleted");

        assertThat(client.deleteUser(7L, "Bearer token")).isEqualTo("deleted");
        assertLastRequest("DELETE", "/api/v1/auth/admin/users/7", null);
    }

    @Test
    void updateUserRoleCallsAuthAdminEndpointWithRoleQueryParam() {
        AuthAdminClient client = clientReturning("updated");

        assertThat(client.updateUserRole(7L, "PLATFORM_ADMIN", "Bearer token")).isEqualTo("updated");
        assertLastRequest("PUT", "/api/v1/auth/admin/users/7/role", "role=PLATFORM_ADMIN");
    }

    @Test
    void suspendAndReactivateCallAuthAdminEndpoints() {
        AuthAdminClient client = clientReturning("ok");

        assertThat(client.suspendUser(7L, "Bearer token")).isEqualTo("ok");
        assertLastRequest("PUT", "/api/v1/auth/admin/users/7/suspend", null);

        assertThat(client.reactivateUser(8L, "Bearer token")).isEqualTo("ok");
        assertLastRequest("PUT", "/api/v1/auth/admin/users/8/reactivate", null);
    }

    private AuthAdminClient clientReturning(String body) {
        ExchangeFunction exchangeFunction = request -> {
            requests.add(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(body)
                    .build());
        };

        return new AuthAdminClient(WebClient.builder().exchangeFunction(exchangeFunction), BASE_URL);
    }

    private void assertLastRequest(String method, String path, String query) {
        ClientRequest request = requests.get(requests.size() - 1);

        assertThat(request.method().name()).isEqualTo(method);
        assertThat(request.url().getPath()).isEqualTo(path);
        assertThat(request.url().getQuery()).isEqualTo(query);
        assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer token");
    }
}
