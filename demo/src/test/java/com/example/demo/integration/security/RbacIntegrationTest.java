package com.example.demo.integration.security;

import com.example.demo.config.SecurityPathsProperties;
import com.example.demo.domain.AuthProvider;
import com.example.demo.domain.Project;
import com.example.demo.domain.Role;
import com.example.demo.domain.User;
import com.example.demo.repository.*;
import com.example.demo.security.JwtAuthenticationEntryPoint;
import com.example.demo.security.RoleAuthorizationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.security.role-endpoints.ADMIN=*:/**",
        "app.security.role-endpoints.MEMBER=GET:/api/**",
        "app.security.role-endpoints.PROJECT_MANAGER=GET:/api/**"
})
@EnableConfigurationProperties(SecurityPathsProperties.class)
class RbacIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private SecurityPathsProperties props;

    @Autowired
    private JwtAuthenticationEntryPoint entryPoint;

    @Autowired
    private ObjectMapper mapper;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User memberUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .addFilters(new RoleAuthorizationFilter(props, entryPoint, mapper))
                .build();

        refreshTokenRepository.deleteAll();
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        memberUser = new User();
        memberUser.setEmail("member-rbac@test.com");
        memberUser.setFullName("Member RBAC");
        memberUser.setRole(Role.MEMBER);
        memberUser.setAuthProvider(AuthProvider.LOCAL);
        memberUser = userRepository.save(memberUser);
    }

    private RequestPostProcessor authenticatedAs(User user) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        user.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                )
        );
    }

    @Test
    void memberToken_hittingAdminOnlyEndpoint_returns403() throws Exception {
        mockMvc.perform(delete("/api/projects/{id}", UUID.randomUUID())
                        .with(authenticatedAs(memberUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminToken_hittingAdminOnlyEndpoint_returns204() throws Exception {
        User adminUser = new User("admin-test@test.com", "pass", "Admin", Role.ADMIN, AuthProvider.LOCAL);
        adminUser = userRepository.save(adminUser);

        Project project = new Project("Test Project", "Desc");
        project.setCreatedBy(adminUser);
        project = projectRepository.save(project);

        mockMvc.perform(delete("/api/projects/{id}", project.getId())
                        .with(authenticatedAs(adminUser)))
                .andExpect(status().isNoContent());
    }
}