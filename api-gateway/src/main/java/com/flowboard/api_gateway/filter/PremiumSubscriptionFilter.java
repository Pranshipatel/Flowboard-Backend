package com.flowboard.api_gateway.filter;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class PremiumSubscriptionFilter extends AbstractGatewayFilterFactory<PremiumSubscriptionFilter.Config> {

    private final WebClient.Builder webClientBuilder;

    public PremiumSubscriptionFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
                return chain.filter(exchange);
            }

            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }

            String userIdHeader = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userIdHeader == null || userIdHeader.isBlank()) {
                return reject(exchange, "Missing X-User-Id header", HttpStatus.UNAUTHORIZED);
            }

            Long userId;
            try {
                userId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                return reject(exchange, "Invalid X-User-Id header", HttpStatus.UNAUTHORIZED);
            }

            return webClientBuilder.build()
                    .get()
                    .uri("lb://PAYMENT-SERVICE/api/v1/subscription/status/{userId}", userId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .flatMap(body -> {
                        String plan = String.valueOf(body.getOrDefault("plan", "FREE"));
                        String status = String.valueOf(body.getOrDefault("status", "EXPIRED"));
                        boolean premiumActive =
                                "PREMIUM".equalsIgnoreCase(plan) && "ACTIVE".equalsIgnoreCase(status);

                        if (!premiumActive) {
                            return reject(exchange, "Premium subscription required", HttpStatus.FORBIDDEN);
                        }
                        return chain.filter(exchange);
                    })
                    .onErrorResume(ex -> reject(exchange, "Unable to validate subscription", HttpStatus.SERVICE_UNAVAILABLE));
        };
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message, HttpStatus status) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"status":%d,"error":"%s","message":"%s"}
                """.formatted(status.value(), status.getReasonPhrase(), message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Data
    public static class Config {
        private boolean enabled = true;
    }
}