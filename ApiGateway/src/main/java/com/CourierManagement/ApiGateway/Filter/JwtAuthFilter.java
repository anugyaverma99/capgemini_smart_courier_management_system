package com.CourierManagement.ApiGateway.Filter;

import com.CourierManagement.ApiGateway.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {
    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        if (isPublicRoute(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return rejectRequest(exchange);
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return rejectRequest(exchange);
        }

        String role = jwtUtil.extractRole(token);
        String email = jwtUtil.extractEmail(token);

        if (path.startsWith("/gateway/admin") && !"ADMIN".equals(role)) {
            return rejectRequest(exchange);
        }

        final String finalEmail = email;
        final String finalRole = role;

        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.set("X-User-Email", finalEmail);
                headers.set("X-User-Role", finalRole);
                return headers;
            }
        };

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(decoratedRequest)
                .build();

        return chain.filter(mutatedExchange);
    }

    private boolean isPublicRoute(String path) {
        return path.contains("/auth/login") ||
               path.contains("/auth/signup") ||
               path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/webjars");
    }

    private Mono<Void> rejectRequest(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}