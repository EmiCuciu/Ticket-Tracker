package com.example.demo.service.implementation;

import com.example.demo.domain.TokenBlacklist;
import com.example.demo.repository.TokenBlacklistRepository;
import com.example.demo.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Override
    @Transactional
    public void blacklist(String jti, Instant expiresAt) {
        TokenBlacklist entry = new TokenBlacklist();
        entry.setJti(jti);
        entry.setExpiresAt(expiresAt);
        tokenBlacklistRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String jti) {
        return tokenBlacklistRepository.existsByJti(jti);
    }

    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        return tokenBlacklistRepository.deleteExpiredTokens(Instant.now());
    }

}