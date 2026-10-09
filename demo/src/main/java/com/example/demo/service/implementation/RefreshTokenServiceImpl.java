package com.example.demo.service.implementation;

import com.example.demo.domain.RefreshToken;
import com.example.demo.domain.User;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Encoder STANDARD_ENCODER = Base64.getEncoder();
    private static final int TOKEN_BYTE_LENGTH = 64;
    private final RefreshTokenRepository refreshTokenRepository;
    @Value("${app.jwt.refresh-token-expiration-days}")
    private long expirationDays;

    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();
        String hash = hash(rawToken);

        RefreshToken refreshToken = new RefreshToken(
                hash, user, Instant.now().plus(expirationDays, ChronoUnit.DAYS)
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional
    public RefreshToken validateAndConsume(String rawToken) {
        String hash = hash(rawToken);

        RefreshToken stored = refreshTokenRepository.findByTokenHashWithUser(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (stored.isRevoked())
            throw new BadCredentialsException("Refresh token revoked");

        if (stored.getExpiresAt().isBefore(Instant.now()))
            throw new BadCredentialsException("Refresh token expired");

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return stored;
    }

    @Transactional
    public void revoke(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashWithUser(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_TOKEN_ENCODER.encodeToString(bytes);
    }

    private String hash(String rawToken) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return STANDARD_ENCODER.encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
