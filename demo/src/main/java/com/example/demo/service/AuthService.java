package com.example.demo.service;

import com.example.demo.model.*;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.UUID;

public interface AuthService {

    /**
     * Registers a new user with the provided registration request.
     *
     * @param request the registration request containing user details
     * @return a response containing information about the registered user
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Authenticates a user with the provided login request.
     *
     * @param request the login request containing user credentials
     * @return a response containing authentication tokens and user information
     */
    AuthResponse login(LoginRequest request);

    /**
     * Refreshes the access and refresh tokens.
     *
     * @param request the refresh token request
     * @return access and refresh tokens with expiration information
     */
    AuthResponse refresh(RefreshTokenRequest request);

    /**
     * Logs out the user by invalidating the provided refresh token and access token.
     *
     * @param request     the refresh token request containing the refresh token to invalidate
     * @param accessToken the access token to invalidate
     */
    void logout(RefreshTokenRequest request, String accessToken);

    /**
     * Logs out the user invalidating all refresh tokens and the current access tokens associated.
     *
     * @param userId      the ID of the user to log out
     * @param accessToken the access token to invalidate
     */
    void logoutAll(UUID userId, String accessToken);

    /**
     * Logs in a user using Google OAuth2 authentication.
     *
     * @param oAuth2User the OAuth2 user information obtained from Google
     * @return a response containing authentication tokens and user information
     */
    AuthResponse loginWithGoogle(OAuth2User oAuth2User);
}
