package com.example.demo.unit.security;

import com.example.demo.domain.Role;
import com.example.demo.domain.User;
import com.example.demo.security.JwtService;
import com.example.demo.service.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Field;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Profile("test")
@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
                "samdiridaidaidaiHaifetitainpoiana".getBytes());
        jwtService = new JwtService(secret, 900_000L, tokenBlacklistService);

        user = new User("user@test.com", "pass", "Test User", Role.MEMBER, com.example.demo.domain.AuthProvider.LOCAL);
        Field idField;
        try {
            idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generateAndExtractUserId() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        assertThat(jwtService.validateToken(token, user.getId())).isEqualTo(true);
    }

    @Test
    void extractJti_isPresentAndUnique() {
        String t1 = jwtService.generateAccessToken(user);
        String t2 = jwtService.generateAccessToken(user);
        assertThat(jwtService.extractJti(t1)).isNotBlank();
        assertThat(jwtService.extractJti(t1)).isNotEqualTo(jwtService.extractJti(t2));
        assertThat(jwtService.validateToken(t1, user.getId())).isTrue();
        assertThat(jwtService.validateToken(t2, user.getId())).isTrue();
    }

    @Test
    void extractExpiration_isInFuture() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.extractExpiration(token)).isAfter(java.time.Instant.now());
    }

    @Test
    void validateToken_valid_returnsTrue() {
        String token = jwtService.generateAccessToken(user);
        when(tokenBlacklistService.isBlacklisted(anyJti())).thenReturn(false);

        assertThat(jwtService.validateToken(token, user.getId())).isTrue();
    }

    @Test
    void validateToken_wrongUserId_returnsFalse() {
        String token = jwtService.generateAccessToken(user);
        assertThat(jwtService.validateToken(token, UUID.randomUUID())).isFalse();
    }

    @Test
    void validateToken_blacklisted_returnsFalse() {
        String token = jwtService.generateAccessToken(user);
        when(tokenBlacklistService.isBlacklisted(anyJti())).thenReturn(true);

        assertThat(jwtService.validateToken(token, user.getId())).isFalse();
    }

    @Test
    void validateToken_expired_returnsFalse() {
        JwtService shortLived = new JwtService(
                Base64.getEncoder().encodeToString("samdiridaidaidaiHaifetitainpoiana".getBytes()),
                -1000L, tokenBlacklistService);
        String token = shortLived.generateAccessToken(user);

        assertThat(shortLived.validateToken(token, user.getId())).isFalse();
    }

    @Test
    void validateToken_tampered_returnsFalse() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 5) + "AAAAA";

        assertThat(jwtService.validateToken(tampered, user.getId())).isFalse();
    }

    @Test
    void extractAllClaims_invalidToken_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(JwtException.class,
                () -> jwtService.extractUserId("not-a-jwt"));
    }

    private String anyJti() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}