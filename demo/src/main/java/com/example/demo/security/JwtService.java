package com.example.demo.security;

import com.example.demo.domain.User;
import com.example.demo.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtService(@Value("${app.jwt.secret-key}") String secret,
                      @Value("${app.jwt.access-token-expiration-ms}") long expirationMs, TokenBlacklistService tokenBlacklistService) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaim(token, Claims::getSubject));
    }

    public Instant extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration).toInstant();
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).isBefore(Instant.now());
    }

    public boolean validateToken(String token, UUID expectedUserId) {
        try {
            if (!extractUserId(token).equals(expectedUserId)) {
                return false;
            }

            if (isTokenExpired(token)) {
                return false;
            }
            return !tokenBlacklistService.isBlacklisted(extractJti(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMinutes() {
        return TimeUnit.MILLISECONDS.toMinutes(expirationMs);
    }
}