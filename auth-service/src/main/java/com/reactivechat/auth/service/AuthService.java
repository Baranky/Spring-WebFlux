package com.reactivechat.auth.service;

import com.reactivechat.auth.dto.LoginRequest;
import com.reactivechat.auth.dto.RegisterRequest;
import com.reactivechat.auth.dto.TokenResponse;
import com.reactivechat.auth.entity.RefreshToken;
import com.reactivechat.auth.entity.User;
import com.reactivechat.auth.repository.RefreshTokenRepository;
import com.reactivechat.auth.repository.UserRepository;
import com.reactivechat.auth.security.JwtTokenFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenFactory jwtTokenFactory;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtTokenFactory jwtTokenFactory,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenFactory = jwtTokenFactory;
        this.passwordEncoder = passwordEncoder;
    }

    public Mono<TokenResponse> register(RegisterRequest request) {
        return userRepository.existsByUsername(request.username())
                .flatMap(exists -> exists ? Mono.error(new IllegalArgumentException("Username already exists")) : Mono.just(true))
                .then(userRepository.existsByEmail(request.email()))
                .flatMap(exists -> exists ? Mono.error(new IllegalArgumentException("Email already exists")) : Mono.just(true))
                .then(Mono.defer(() -> {
                    User user = new User();
                    user.setUsername(request.username());
                    user.setPasswordHash(passwordEncoder.encode(request.password()));
                    user.setEmail(request.email());
                    user.setRoles(java.util.Set.of("USER"));
                    return userRepository.save(user).flatMap(this::issueTokens);
                }));
    }

    public Mono<TokenResponse> login(LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid credentials")))
                .flatMap(this::issueTokens);
    }

    public Mono<TokenResponse> refresh(String refreshTokenValue) {
        return refreshTokenRepository.findByToken(refreshTokenValue)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid or expired refresh token")))
                .flatMap(t -> userRepository.findById(t.getUserId()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                .flatMap(user -> {
                    return refreshTokenRepository.deleteByUserId(user.getId()).thenReturn(user);
                })
                .flatMap(this::issueTokens);
    }

    private Mono<TokenResponse> issueTokens(User user) {
        List<String> rolesList = List.copyOf(user.getRoles());
        String accessToken = jwtTokenFactory.createAccessToken(user.getId(), user.getUsername(), rolesList);
        String refreshToken = jwtTokenFactory.createRefreshToken(user.getId());

        RefreshToken rt = new RefreshToken();
        rt.setId(UUID.randomUUID().toString());
        rt.setToken(refreshToken);
        rt.setUserId(user.getId());
        rt.setCreatedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plusSeconds(604800));

        return refreshTokenRepository.save(rt)
                .thenReturn(TokenResponse.of(
                        accessToken,
                        refreshToken,
                        jwtTokenFactory.getAccessTokenValiditySeconds(),
                        user.getId(),
                        user.getUsername(),
                        user.getRoles()
                ));
    }
}
