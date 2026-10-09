package com.example.demo.service.implementation;

import com.example.demo.domain.AuthProvider;
import com.example.demo.domain.RefreshToken;
import com.example.demo.domain.Role;
import com.example.demo.domain.User;
import com.example.demo.exception.EmailAlreadyExistsException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.*;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.AuthService;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {


        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new EmailAlreadyExistsException(request.getEmail());
        });

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                Role.MEMBER,
                AuthProvider.LOCAL
        );

        user = userRepository.save(user);

        RegisterResponse response = new RegisterResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(com.example.demo.model.Role.valueOf(user.getRole().name()));
        return response;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return issueTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenService.validateAndConsume(request.getRefreshToken());
        User user = refreshToken.getUser();

        return issueTokens(user);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request, String accessToken) {
        refreshTokenService.revoke(request.getRefreshToken());

        tokenBlacklistService.blacklist(jwtService.extractJti(accessToken), jwtService.extractExpiration(accessToken));
    }

    @Override
    @Transactional
    public void logoutAll(UUID userId, String accessToken) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        refreshTokenService.revokeAllForUser(userId);
        tokenBlacklistService.blacklist(jwtService.extractJti(accessToken), jwtService.extractExpiration(accessToken));
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String googleSub = oAuth2User.getAttribute("sub");
        String fullName = oAuth2User.getAttribute("name");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        if (email == null || googleSub == null) {
            throw new IllegalArgumentException("Google account missing required attributes (email or sub)");
        }

        if (emailVerified == null || !emailVerified) {
            throw new IllegalStateException("Google email is not verified. Cannot proceed.");
        }

        Optional<User> byGoogleSub = userRepository.findByGoogleSub(googleSub);
        if (byGoogleSub.isPresent()) {
            return issueTokens(byGoogleSub.get());
        }

        Optional<User> byEmail = userRepository.findByEmail(email);

        if (byEmail.isPresent()) {
            User existing = byEmail.get();

            if (existing.getAuthProvider() == AuthProvider.LOCAL) {
                existing.setAuthProvider(AuthProvider.GOOGLE);
                existing.setGoogleSub(googleSub);
                userRepository.save(existing);
                return issueTokens(existing);
            }

            existing.setGoogleSub(googleSub);
            userRepository.save(existing);
            return issueTokens(existing);
        }

        User newUser = new User(
                email,
                null,
                fullName,
                Role.MEMBER,
                AuthProvider.GOOGLE
        );
        newUser.setGoogleSub(googleSub);
        newUser = userRepository.save(newUser);

        return issueTokens(newUser);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtService.getExpirationMinutes());
        return response;
    }
}