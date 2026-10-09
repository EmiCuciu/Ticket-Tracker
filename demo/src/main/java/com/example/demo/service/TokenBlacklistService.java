package com.example.demo.service;

import java.time.Instant;

public interface TokenBlacklistService {

    /**
     * Blacklists a token by its JTI (JWT ID).
     *
     * @param jti       the JWT ID of the token to be blacklisted
     * @param expiresAt the expiration time of the token
     */
    void blacklist(String jti, Instant expiresAt);

    /**
     * Checks if a token is blacklisted by its JTI.
     *
     * @param jti the JWT ID of the token to check
     * @return true if the token is blacklisted, false otherwise
     */
    boolean isBlacklisted(String jti);

    /**
     * Cleans up expired tokens from the blacklist.
     *
     * @return the number of tokens removed from the blacklist
     */
    int cleanupExpiredTokens();
}