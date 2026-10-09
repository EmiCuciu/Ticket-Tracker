package com.example.demo.unit.repository.specification;

import com.example.demo.domain.*;
import com.example.demo.dto.TicketSearchRequestDto;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.spec.TicketSpecification;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TicketSpecification.class)
@TestPropertySource(properties = {
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///ticket_tracket_test_db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class TicketSpecificationTest {

    private final Pageable pageable = PageRequest.of(0, 10);
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TicketRepository ticketRepository;
    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setRole(Role.MEMBER);
        user.setAuthProvider(AuthProvider.LOCAL);
        userRepository.save(user);

        Project project = new Project();
        project.setName("Test Project");
        project.setCreatedBy(user);
        projectRepository.save(project);

        Ticket ticket1 = new Ticket();
        ticket1.setTitle("Ticket1");
        ticket1.setStatus(Status.OPEN);
        ticket1.setPriority(Priority.HIGH);
        ticket1.setProject(project);
        ticket1.setAssignee(user);
        ticket1.setDueDate(Instant.now().plus(3, ChronoUnit.DAYS));
        ticketRepository.save(ticket1);

        Ticket ticket2 = new Ticket();
        ticket2.setTitle("Ticket2");
        ticket2.setStatus(Status.DONE);
        ticket2.setPriority(Priority.LOW);
        ticket2.setProject(project);
        ticket2.setDueDate(Instant.now().plus(10, ChronoUnit.DAYS));
        ticketRepository.save(ticket2);

        Ticket ticket3 = new Ticket();
        ticket3.setTitle("Ticket3 - Refactor");
        ticket3.setStatus(Status.OPEN);
        ticket3.setPriority(Priority.LOW);
        ticket3.setProject(project);
        ticket3.setDueDate(Instant.now().plus(1, ChronoUnit.DAYS));
        ticketRepository.save(ticket3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void filterByStatus() {
        TicketSearchRequestDto request = new TicketSearchRequestDto(
                Status.OPEN,
                null,
                null,
                null,
                null,
                null
        );

        Page<Ticket> result = ticketRepository.findAll(TicketSpecification.build(request), pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(ticket -> ticket.getStatus() == Status.OPEN);
    }

    @Test
    void filterByStatusAndPriority() {
        TicketSearchRequestDto request = new TicketSearchRequestDto(
                Status.OPEN,
                Priority.HIGH,
                null,
                null,
                null,
                null
        );

        Page<Ticket> result = ticketRepository.findAll(TicketSpecification.build(request), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Ticket1");
    }

    @Test
    void noFilters() {
        TicketSearchRequestDto request = new TicketSearchRequestDto(
                null,
                null,
                null,
                null,
                null,
                null
        );

        Page<Ticket> result = ticketRepository.findAll(TicketSpecification.build(request), pageable);

        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    void dueDate_Range() {
        TicketSearchRequestDto request = new TicketSearchRequestDto(
                null,
                null,
                null,
                Instant.now(),
                Instant.now().plus(5, ChronoUnit.DAYS),
                null
        );

        Page<Ticket> result = ticketRepository.findAll(TicketSpecification.build(request), pageable);

        assertThat(result.getContent())
                .extracting(Ticket::getTitle)
                .containsExactlyInAnyOrder("Ticket1", "Ticket3 - Refactor");
    }

    @Test
    void combinedThreeFilters() {
        TicketSearchRequestDto request = new TicketSearchRequestDto(
                Status.OPEN,
                Priority.LOW,
                null,
                null,
                Instant.now().plus(5, ChronoUnit.DAYS),
                null
        );

        Page<Ticket> result = ticketRepository.findAll(TicketSpecification.build(request), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Ticket3 - Refactor");
    }

    @Test
    void filterByAssigneeOnly() {
        User otherUser = new User();
        otherUser.setEmail("other@test.com");
        otherUser.setFullName("Other");
        otherUser.setRole(Role.MEMBER);
        otherUser.setAuthProvider(AuthProvider.LOCAL);
        userRepository.save(otherUser);

        TicketSearchRequestDto request = new TicketSearchRequestDto(
                null, null, otherUser.getId(), null, null, null);

        Page<Ticket> result = ticketRepository.findAll(TicketSpecification.build(request), pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void filterByTitleContainsOnly() {
        TicketSearchRequestDto request = new TicketSearchRequestDto(
                null, null, null, null, null, "refactor");

        Page<Ticket> result = ticketRepository.findAll(TicketSpecification.build(request), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).contains("Refactor");
    }

    @Test
    void filterByDueAfterOnly() {
        TicketSearchRequestDto request = new TicketSearchRequestDto(
                null, null, null, Instant.now().plus(5, ChronoUnit.DAYS), null, null);

        Page<Ticket> result = ticketRepository.findAll(TicketSpecification.build(request), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Ticket2");
    }
}
