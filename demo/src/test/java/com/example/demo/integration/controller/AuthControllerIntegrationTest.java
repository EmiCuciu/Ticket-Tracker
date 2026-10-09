package com.example.demo.integration.controller;

import com.example.demo.model.LoginRequest;
import com.example.demo.model.RefreshTokenRequest;
import com.example.demo.model.RegisterRequest;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullAuthFlow_registerLoginProtectedRefreshLogout_oldTokenRejected() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("authflow@test.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setFullName("Auth Flow User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("authflow@test.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("authflow@test.com");
        loginRequest.setPassword("Password123!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.get("accessToken").asString();
        String refreshToken = loginBody.get("refreshToken").asString();

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newAccessToken = refreshBody.get("accessToken").asString();
        String newRefreshToken = refreshBody.get("refreshToken").asString();

        assertThat(newAccessToken).isNotEqualTo(accessToken);
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // Reusing rotated refresh token must fail with 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());

        RefreshTokenRequest logoutRequest = new RefreshTokenRequest();
        logoutRequest.setRefreshToken(newRefreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAll_revokesAllPreviouslyIssuedRefreshTokens() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("logoutall@test.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setFullName("Logout All User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("logoutall@test.com");
        loginRequest.setPassword("Password123!");

        MvcResult login1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult login2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body1 = objectMapper.readTree(login1.getResponse().getContentAsString());
        JsonNode body2 = objectMapper.readTree(login2.getResponse().getContentAsString());

        String accessToken1 = body1.get("accessToken").asString();
        String refresh1 = body1.get("refreshToken").asString();
        String refresh2 = body2.get("refreshToken").asString();

        mockMvc.perform(post("/api/auth/logout-all")
                        .header("Authorization", "Bearer " + accessToken1))
                .andExpect(status().isNoContent());

        RefreshTokenRequest req1 = new RefreshTokenRequest();
        req1.setRefreshToken(refresh1);

        RefreshTokenRequest req2 = new RefreshTokenRequest();
        req2.setRefreshToken(refresh2);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isUnauthorized());
    }
}