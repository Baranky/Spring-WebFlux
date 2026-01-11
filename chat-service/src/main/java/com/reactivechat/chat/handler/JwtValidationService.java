package com.reactivechat.chat.handler;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Service
public class JwtValidationService {

    private final SecretKey secretKey;

    public JwtValidationService(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Mono<String> validateAndGetUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Mono.error(new IllegalArgumentException("Missing or invalid Authorization"));
        }
        String token = authorization.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            if (!"access".equals(claims.get("type"))) {
                return Mono.error(new IllegalArgumentException("Invalid token type"));
            }
            return Mono.just(claims.getSubject());
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Invalid token", e));
        }
    }
}
