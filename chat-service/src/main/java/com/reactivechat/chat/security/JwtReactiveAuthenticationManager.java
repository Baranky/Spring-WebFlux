package com.reactivechat.chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final SecretKey secretKey;

    public JwtReactiveAuthenticationManager(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication.getCredentials())
                .cast(String.class)
                .filter(token -> !token.isBlank())
                .flatMap(token -> {
                    try {
                        Claims claims = Jwts.parserBuilder()
                                .setSigningKey(secretKey)
                                .build()
                                .parseClaimsJws(token)
                                .getBody();
                        if (!"access".equals(claims.get("type"))) {
                            return Mono.empty();
                        }
                        String userId = claims.getSubject();
                        @SuppressWarnings("unchecked")
                        List<String> roles = claims.get("roles", List.class);
                        if (roles == null) roles = List.of();
                        var authorities = roles.stream()
                                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                                .collect(Collectors.toList());
                        return Mono.just((Authentication) new UsernamePasswordAuthenticationToken(userId, token, authorities));
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                });
    }
}
