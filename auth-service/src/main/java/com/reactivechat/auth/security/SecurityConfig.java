package com.reactivechat.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
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
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .authorizeExchange(authorize -> authorize
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .pathMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                        .pathMatchers("/auth/**").authenticated()
                )
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
