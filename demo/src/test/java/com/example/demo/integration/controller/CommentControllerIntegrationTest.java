package com.example.demo.integration.controller;

import com.example.demo.domain.*;
import com.example.demo.model.CommentCreateRequest;
import com.example.demo.repository.*;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Ticket ticket;

    private User user;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        user = new User();
        user.setEmail("integration@test.com");
        user.setFullName("Integration User");
        user.setRole(Role.MEMBER);
        user.setAuthProvider(AuthProvider.LOCAL);
        user = userRepository.save(user);

        Project project = new Project();
        project.setName("Integration Project");
        project.setCreatedBy(user);
        project = projectRepository.save(project);

        ticket = new Ticket();
        ticket.setTitle("Integration Ticket");
        ticket.setStatus(Status.OPEN);
        ticket.setPriority(Priority.MEDIUM);
        ticket.setProject(project);
        ticket = ticketRepository.save(ticket);
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
    void commentLifecycle() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBody("Integration comment");

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .with(authenticatedUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Integration comment"))
                .andExpect(jsonPath("$.ticketId").value(ticket.getId().toString()))
                .andExpect(jsonPath("$.author.fullName").value("Integration User"));

        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].body").value("Integration comment"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void createComment_blankBody_returns400() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBody("");

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createComment_ticketNotFound_returns404() throws Exception {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBody("Comment");

        mockMvc.perform(post("/api/tickets/{ticketId}/comments", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getComments_ticketNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getComments_ticketHasNoComments_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/tickets/{ticketId}/comments", ticket.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}