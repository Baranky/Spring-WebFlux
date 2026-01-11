package com.reactivechat.auth.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

public class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(h -> h.startsWith(BEARER_PREFIX))
                .map(h -> h.substring(BEARER_PREFIX.length()))
                .map(token -> (Authentication) new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        null, token, java.util.Collections.emptyList()));
    }
}
