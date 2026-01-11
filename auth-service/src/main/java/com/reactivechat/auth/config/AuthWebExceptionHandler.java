package com.reactivechat.auth.config;

import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;


@Component
@Order(-2)
public class AuthWebExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        IllegalArgumentException iae = unwrap(IllegalArgumentException.class, ex);
        if (iae == null) {
            return Mono.error(ex);
        }
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String message = iae.getMessage() != null ? iae.getMessage() : "Bad request";
        String body = "{\"error\":\"" + escapeJson(message) + "\"}";
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static <T extends Throwable> T unwrap(Class<T> type, Throwable ex) {
        while (ex != null) {
            if (type.isInstance(ex)) {
                return type.cast(ex);
            }
            ex = ex.getCause();
        }
        return null;
    }
}
