package com.reactivechat.chat.config;

import com.reactivechat.chat.security.JwtReactiveAuthenticationManager;
import com.reactivechat.chat.security.JwtServerAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtReactiveAuthenticationManager jwtReactiveAuthenticationManager;
    private final JwtServerAuthenticationConverter jwtServerAuthenticationConverter;

    public SecurityConfig(JwtReactiveAuthenticationManager jwtReactiveAuthenticationManager,
                          JwtServerAuthenticationConverter jwtServerAuthenticationConverter) {
        this.jwtReactiveAuthenticationManager = jwtReactiveAuthenticationManager;
        this.jwtServerAuthenticationConverter = jwtServerAuthenticationConverter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(jwtReactiveAuthenticationManager);
        jwtFilter.setServerAuthenticationConverter(jwtServerAuthenticationConverter);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/ws/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().denyAll()
                )
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
