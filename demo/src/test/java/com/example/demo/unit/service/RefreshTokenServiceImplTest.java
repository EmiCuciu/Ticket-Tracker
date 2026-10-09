package com.example.demo.unit.service;

import com.example.demo.domain.RefreshToken;
import com.example.demo.domain.User;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.service.implementation.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "expirationDays", 7L);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
    }

    @Test
    void issue_savesHashedTokenAndReturnsRawToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String rawToken = refreshTokenService.issue(user);

        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(rawToken).isNotBlank();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().plus(6, ChronoUnit.DAYS));
        assertThat(saved.isRevoked()).isFalse();
    }

    @Test
    void issue_generatesDifferentTokensOnEachCall() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String token1 = refreshTokenService.issue(user);
        String token2 = refreshTokenService.issue(user);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void validateAndConsume_validToken_revokesAndReturnsIt() {
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        stored.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken result = refreshTokenService.validateAndConsume("any-raw-token");

        assertThat(result).isSameAs(stored);
        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void validateAndConsume_unknownToken_throwsBadCredentials() {
        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.empty());

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> refreshTokenService.validateAndConsume("garbage"))
                .withMessage("Invalid refresh token");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void validateAndConsume_revokedToken_throwsBadCredentials() {
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        stored.setRevoked(true);

        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.of(stored));

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> refreshTokenService.validateAndConsume("token"))
                .withMessage("Refresh token revoked");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void validateAndConsume_expiredToken_throwsBadCredentials() {
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        stored.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.of(stored));

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> refreshTokenService.validateAndConsume("token"))
                .withMessage("Refresh token expired");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revoke_existingToken_setsRevokedTrue() {
        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setRevoked(false);

        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.revoke("token");

        assertThat(stored.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(stored);
    }

    @Test
    void revoke_unknownToken_throwsBadCredentials() {
        when(refreshTokenRepository.findByTokenHashWithUser(any())).thenReturn(Optional.empty());

        assertThatExceptionOfType(BadCredentialsException.class)
                .isThrownBy(() -> refreshTokenService.revoke("garbage"));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void revokeAllForUser_delegatesToRepository() {
        UUID userId = UUID.randomUUID();

        refreshTokenService.revokeAllForUser(userId);

        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }
}