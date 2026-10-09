package com.example.demo.service;

import com.example.demo.domain.RefreshToken;
import com.example.demo.domain.User;

import java.util.UUID;

public interface RefreshTokenService {

    /**
     * Creates a new refresh token for the given user.
     *
     * @param user the user for whom the refresh token is to be created
     * @return the created RefreshToken object
     */
    String issue(User user);

    /**
     * Validates the provided raw refresh token and consumes it if valid.
     *
     * @param rawToken the raw refresh token to validate and consume
     * @return the validated RefreshToken object
     * @throws IllegalArgumentException if the token is invalid or expired
     */
    RefreshToken validateAndConsume(String rawToken);

    /**
     * Revokes the provided raw refresh token, making it invalid for future use.
     *
     * @param rawToken the raw refresh token to revoke
     */
    void revoke(String rawToken);

    /**
     * Revokes all refresh tokens associated with the specified user.
     *
     * @param userId the ID of the user whose refresh tokens are to be revoked
     */
    void revokeAllForUser(UUID userId);
}
