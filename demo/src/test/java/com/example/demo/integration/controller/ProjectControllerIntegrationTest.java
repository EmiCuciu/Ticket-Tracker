package com.example.demo.integration.controller;

import com.example.demo.domain.AuthProvider;
import com.example.demo.domain.Role;
import com.example.demo.domain.User;
import com.example.demo.model.ProjectCreateRequest;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private User user;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        commentRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("integration@test.com");
        user.setFullName("Integration User");
        user.setRole(Role.MEMBER);
        user.setAuthProvider(AuthProvider.LOCAL);
        userRepository.save(user);

    }

    private RequestPostProcessor authenticatedUser(User u) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        u.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
                )
        );
    }

    @Test
    void projectCrudLifecycle() throws Exception {
        ProjectCreateRequest createRequest = new ProjectCreateRequest();
        createRequest.setName("Integration Project");
        createRequest.setDescription("Integration description");

        String createResponse = mockMvc.perform(post("/api/projects")
                        .with(authenticatedUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Integration Project"))
                .andExpect(jsonPath("$.createdBy.fullName").value("Integration User"))
                .andReturn().getResponse().getContentAsString();

        UUID projectId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asString());

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/api/projects/{id}", projectId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProject_missingName_returns400() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setDescription("no name");

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getProject_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/projects/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}