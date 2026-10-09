package com.example.demo.integration.controller;

import com.example.demo.domain.*;
import com.example.demo.model.Priority;
import com.example.demo.model.Status;
import com.example.demo.model.TicketCreateRequest;
import com.example.demo.model.TicketPatchRequest;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketControllerIntegrationTest {

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

    private User user;
    private Project project;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;


    private RequestPostProcessor authenticatedUser(User u) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        u.getId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))));
    }

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
        user.setRole(Role.ADMIN);
        user.setAuthProvider(AuthProvider.LOCAL);
        user = userRepository.save(user);

        project = new Project();
        project.setName("Integration Project");
        project.setCreatedBy(user);
        project = projectRepository.save(project);
    }

    @Test
    void ticketCrudLifecycle() throws Exception {
        TicketCreateRequest createRequest = new TicketCreateRequest();
        createRequest.setTitle("Integration ticket");
        createRequest.setDescription("Integration description");
        createRequest.setStatus(Status.OPEN);
        createRequest.setPriority(Priority.MEDIUM);
        createRequest.setProjectId(project.getId());
        createRequest.setAssigneeId(JsonNullable.of(user.getId()));

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration ticket"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();

        UUID ticketId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asString());

        mockMvc.perform(get("/api/tickets/{id}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.title").value("Integration ticket"));

        TicketPatchRequest patchRequest = new TicketPatchRequest();
        patchRequest.setStatus(JsonNullable.of(Status.DONE));

        mockMvc.perform(patch("/api/tickets/{id}", ticketId)
                        .with(authenticatedUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.title").value("Integration ticket"));

        mockMvc.perform(delete("/api/tickets/{id}", ticketId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tickets/{id}", ticketId))
                .andExpect(status().isNotFound());
    }

    @Test
    void multiFilterCriteriaQueryTest() throws Exception {
        Ticket t1 = new Ticket();
        t1.setTitle("Urgent bug");
        t1.setStatus(com.example.demo.domain.Status.OPEN);
        t1.setPriority(com.example.demo.domain.Priority.HIGH);
        t1.setProject(project);
        t1.setAssignee(user);
        t1.setDueDate(Instant.now().plus(2, ChronoUnit.DAYS));
        ticketRepository.save(t1);

        Ticket t2 = new Ticket();
        t2.setTitle("Low priority task");
        t2.setStatus(com.example.demo.domain.Status.OPEN);
        t2.setPriority(com.example.demo.domain.Priority.LOW);
        t2.setProject(project);
        t2.setDueDate(Instant.now().plus(10, ChronoUnit.DAYS));
        ticketRepository.save(t2);

        Ticket t3 = new Ticket();
        t3.setTitle("Done task");
        t3.setStatus(com.example.demo.domain.Status.DONE);
        t3.setPriority(com.example.demo.domain.Priority.HIGH);
        t3.setProject(project);
        t3.setDueDate(Instant.now().plus(2, ChronoUnit.DAYS));
        ticketRepository.save(t3);

        mockMvc.perform(get("/api/tickets")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("dueBefore", Instant.now().plus(5, ChronoUnit.DAYS).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Urgent bug"));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void pagination_returnsCorrectPage() throws Exception {
        for (int i = 0; i < 5; i++) {
            Ticket t = new Ticket();
            t.setTitle("Ticket " + i);
            t.setStatus(com.example.demo.domain.Status.OPEN);
            t.setPriority(com.example.demo.domain.Priority.MEDIUM);
            t.setProject(project);
            ticketRepository.save(t);
        }

        mockMvc.perform(get("/api/tickets").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void sorting_byDueDate_ascending() throws Exception {
        Ticket earlier = new Ticket();
        earlier.setTitle("Earlier");
        earlier.setStatus(com.example.demo.domain.Status.OPEN);
        earlier.setPriority(com.example.demo.domain.Priority.LOW);
        earlier.setProject(project);
        earlier.setDueDate(Instant.now().plus(1, ChronoUnit.DAYS));
        ticketRepository.save(earlier);

        Ticket later = new Ticket();
        later.setTitle("Later");
        later.setStatus(com.example.demo.domain.Status.OPEN);
        later.setPriority(com.example.demo.domain.Priority.LOW);
        later.setProject(project);
        later.setDueDate(Instant.now().plus(5, ChronoUnit.DAYS));
        ticketRepository.save(later);

        mockMvc.perform(get("/api/tickets")
                        .param("sortBy", "dueDate")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Earlier"))
                .andExpect(jsonPath("$.content[1].title").value("Later"));
    }

    @Test
    void sorting_byDueDate_descending() throws Exception {
        Ticket earlier = new Ticket();
        earlier.setTitle("Earlier");
        earlier.setStatus(com.example.demo.domain.Status.OPEN);
        earlier.setPriority(com.example.demo.domain.Priority.LOW);
        earlier.setProject(project);
        earlier.setDueDate(Instant.now().plus(1, ChronoUnit.DAYS));
        ticketRepository.save(earlier);

        Ticket later = new Ticket();
        later.setTitle("Later");
        later.setStatus(com.example.demo.domain.Status.OPEN);
        later.setPriority(com.example.demo.domain.Priority.LOW);
        later.setProject(project);
        later.setDueDate(Instant.now().plus(5, ChronoUnit.DAYS));
        ticketRepository.save(later);

        mockMvc.perform(get("/api/tickets")
                        .param("sortBy", "dueDate")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Later"))
                .andExpect(jsonPath("$.content[1].title").value("Earlier"));
    }

    @Test
    void sorting_byPriority_descendingByDefault() throws Exception {
        Ticket low = new Ticket();
        low.setTitle("Low prio");
        low.setStatus(com.example.demo.domain.Status.OPEN);
        low.setPriority(com.example.demo.domain.Priority.LOW);
        low.setProject(project);
        ticketRepository.save(low);

        Ticket urgent = new Ticket();
        urgent.setTitle("Urgent prio");
        urgent.setStatus(com.example.demo.domain.Status.OPEN);
        urgent.setPriority(com.example.demo.domain.Priority.URGENT);
        urgent.setProject(project);
        ticketRepository.save(urgent);

        Ticket medium = new Ticket();
        medium.setTitle("Medium prio");
        medium.setStatus(com.example.demo.domain.Status.OPEN);
        medium.setPriority(com.example.demo.domain.Priority.MEDIUM);
        medium.setProject(project);
        ticketRepository.save(medium);

        mockMvc.perform(get("/api/tickets").param("sortBy", "priority"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Urgent prio"))
                .andExpect(jsonPath("$.content[1].title").value("Medium prio"))
                .andExpect(jsonPath("$.content[2].title").value("Low prio"));
    }

    @Test
    void sorting_byPriority_ascending() throws Exception {
        Ticket low = new Ticket();
        low.setTitle("Low prio");
        low.setStatus(com.example.demo.domain.Status.OPEN);
        low.setPriority(com.example.demo.domain.Priority.LOW);
        low.setProject(project);
        ticketRepository.save(low);

        Ticket urgent = new Ticket();
        urgent.setTitle("Urgent prio");
        urgent.setStatus(com.example.demo.domain.Status.OPEN);
        urgent.setPriority(com.example.demo.domain.Priority.URGENT);
        urgent.setProject(project);
        ticketRepository.save(urgent);

        mockMvc.perform(get("/api/tickets")
                        .param("sortBy", "priority")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Low prio"))
                .andExpect(jsonPath("$.content[1].title").value("Urgent prio"));
    }

    @Test
    void patchTicket_setsAssigneeAndDueDate_whenPresentInJson() throws Exception {
        TicketCreateRequest createRequest = new TicketCreateRequest();
        createRequest.setTitle("Ticket to patch");
        createRequest.setStatus(Status.OPEN);
        createRequest.setPriority(Priority.LOW);
        createRequest.setProjectId(project.getId());

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID ticketId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asString());

        TicketPatchRequest patchRequest = new TicketPatchRequest();
        patchRequest.setAssigneeId(JsonNullable.of(user.getId()));
        patchRequest.setDueDate(JsonNullable.of(Instant.now().plus(3, ChronoUnit.DAYS)));

        mockMvc.perform(patch("/api/tickets/{id}", ticketId)
                        .with(authenticatedUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.dueDate").isNotEmpty());
    }

    @Test
    void patchTicket_clearsAssignee_whenAssigneeIdExplicitlyNull() throws Exception {
        TicketCreateRequest createRequest = new TicketCreateRequest();
        createRequest.setTitle("Ticket with assignee");
        createRequest.setStatus(Status.OPEN);
        createRequest.setPriority(Priority.LOW);
        createRequest.setProjectId(project.getId());
        createRequest.setAssigneeId(JsonNullable.of(user.getId()));

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID ticketId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asString());

        String patchJson = "{\"assigneeId\": null}";

        mockMvc.perform(patch("/api/tickets/{id}", ticketId)
                        .with(authenticatedUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").doesNotExist());
    }

    @Test
    void patchTicket_leavesAssigneeUnchanged_whenAssigneeIdAbsentFromJson() throws Exception {
        TicketCreateRequest createRequest = new TicketCreateRequest();
        createRequest.setTitle("Ticket with assignee");
        createRequest.setStatus(Status.OPEN);
        createRequest.setPriority(Priority.LOW);
        createRequest.setProjectId(project.getId());
        createRequest.setAssigneeId(JsonNullable.of(user.getId()));

        String createResponse = mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID ticketId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asString());

        String patchJson = "{\"status\": \"DONE\"}"; // assigneeId absent

        mockMvc.perform(patch("/api/tickets/{id}", ticketId)
                        .with(authenticatedUser(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void patchTicket_memberNotAssignee_returns403() throws Exception {
        User other = new User();
        other.setEmail("other@test.com");
        other.setFullName("Other");
        other.setRole(Role.MEMBER);
        other.setAuthProvider(AuthProvider.LOCAL);
        other = userRepository.save(other);

        Ticket t = new Ticket();
        t.setTitle("Assigned to user");
        t.setStatus(com.example.demo.domain.Status.OPEN);
        t.setPriority(com.example.demo.domain.Priority.LOW);
        t.setProject(project);
        t.setAssignee(user);
        t = ticketRepository.save(t);

        mockMvc.perform(patch("/api/tickets/{id}", t.getId())
                        .with(authenticatedUser(other))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DONE\"}"))
                .andExpect(status().isForbidden());
    }
}