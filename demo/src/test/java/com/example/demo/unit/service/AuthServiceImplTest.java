package com.example.demo.unit.service;

import com.example.demo.domain.RefreshToken;
import com.example.demo.domain.User;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.AuthResponse;
import com.example.demo.model.RefreshTokenRequest;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.TokenBlacklistService;
import com.example.demo.service.implementation.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @InjectMocks
    private AuthServiceImpl authService;

    private User user;
    private RefreshToken storedRefreshToken;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");

        storedRefreshToken = new RefreshToken();
        storedRefreshToken.setUser(user);
    }

    @Test
    void refresh_validToken_returnsNewPair() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("old-raw-token");

        when(refreshTokenService.validateAndConsume("old-raw-token")).thenReturn(storedRefreshToken);
        when(jwtService.generateAccessToken(user)).thenReturn("new-access-token");
        when(refreshTokenService.issue(user)).thenReturn("new-refresh-token");
        when(jwtService.getExpirationMinutes()).thenReturn(15L);

        AuthResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(15L);

        verify(refreshTokenService).validateAndConsume("old-raw-token");
        verify(jwtService).generateAccessToken(user);
        verify(refreshTokenService).issue(user);
        verify(jwtService).getExpirationMinutes();
        verifyNoMoreInteractions(refreshTokenService, jwtService);
    }

    @Test
    void logout_delegatesToRevokeAndBlacklist() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token-to-revoke");

        when(jwtService.extractJti("dummy-access-token")).thenReturn("jti-123");
        when(jwtService.extractExpiration("dummy-access-token"))
                .thenReturn(Instant.now().plusSeconds(900));

        authService.logout(request, "dummy-access-token");

        verify(refreshTokenService).revoke("token-to-revoke");
        verify(tokenBlacklistService).blacklist(eq("jti-123"), any(Instant.class));
        verifyNoMoreInteractions(refreshTokenService, tokenBlacklistService);
    }

    @Test
    void logoutAll_userExists_revokesAllAndBlacklists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(jwtService.extractJti("dummy-access-token")).thenReturn("jti-456");
        when(jwtService.extractExpiration("dummy-access-token"))
                .thenReturn(Instant.now().plusSeconds(900));

        authService.logoutAll(userId, "dummy-access-token");

        verify(userRepository).existsById(userId);
        verify(refreshTokenService).revokeAllForUser(userId);
        verify(tokenBlacklistService).blacklist(eq("jti-456"), any(Instant.class));
        verifyNoMoreInteractions(userRepository, refreshTokenService, tokenBlacklistService);
    }

    @Test
    void logoutAll_userNotFound_throws() {
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatExceptionOfType(UserNotFoundException.class)
                .isThrownBy(() -> authService.logoutAll(userId, "dummy-access-token"));

        verify(userRepository).existsById(userId);
        verifyNoInteractions(refreshTokenService);
    }
}