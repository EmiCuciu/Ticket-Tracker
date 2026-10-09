package com.example.demo.controller;

import com.example.demo.api.AuthApi;
import com.example.demo.model.*;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

    private final AuthService authService;

    private final CurrentUserProvider currentUserProvider;

    public AuthController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public ResponseEntity<RegisterResponse> register(RegisterRequest registerRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerRequest));
    }

    @Override
    public ResponseEntity<AuthResponse> login(LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @Override
    public ResponseEntity<AuthResponse> refresh(RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authService.refresh(refreshTokenRequest));
    }

    @Override
    public ResponseEntity<Void> logout(RefreshTokenRequest refreshTokenRequest) {
        authService.logout(refreshTokenRequest, currentUserProvider.getCurrentAccessToken());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> logoutAll() {
        authService.logoutAll(currentUserProvider.getCurrentUserId(), currentUserProvider.getCurrentAccessToken());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> googleLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/oauth2/authorization/google")
                .build();
    }
}