package com.reactivechat.auth.security;

import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtReactiveAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtTokenFactory jwtTokenFactory;

    public JwtReactiveAuthenticationManager(JwtTokenFactory jwtTokenFactory) {
        this.jwtTokenFactory = jwtTokenFactory;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication.getCredentials())
                .cast(String.class)
                .filter(token -> !token.isBlank())
                .flatMap(token -> {
                    try {
                        var claims = jwtTokenFactory.parseToken(token);
                        if (!"access".equals(claims.get("type"))) {
                            return Mono.empty();
                        }
                        String userId = claims.getSubject();
                        String username = claims.get("username", String.class);
                        List<String> roles = jwtTokenFactory.getRoles(claims);
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
