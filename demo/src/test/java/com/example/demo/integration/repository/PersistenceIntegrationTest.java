package com.example.demo.integration.repository;

import com.example.demo.domain.*;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.test.database.replace=none",
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///ticket_tracket_test_db",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class PersistenceIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void canPersistAndRetrieve() {
        User user1 = new User();
        user1.setEmail("emicuciu@gmail.com");
        user1.setPassword("emi12345");
        user1.setFullName("Cuciurean Emilian");
        user1.setRole(Role.ADMIN);
        user1.setAuthProvider(AuthProvider.LOCAL);
        userRepository.save(user1);

        User user2 = new User();
        user2.setEmail("varvara@gmail.com");
        user2.setFullName("Nastase Varvara");
        user2.setRole(Role.MEMBER);
        user2.setAuthProvider(AuthProvider.GOOGLE);
        userRepository.save(user2);

        Project project1 = new Project();
        project1.setName("Project1");
        project1.setDescription("description1");
        project1.setCreatedBy(user1);
        projectRepository.save(project1);

        Ticket ticket1 = new Ticket();
        ticket1.setTitle("Ticket1");
        ticket1.setStatus(Status.IN_PROGRESS);
        ticket1.setPriority(Priority.HIGH);
        ticket1.setProject(project1);
        ticket1.setAssignee(user2);
        ticket1.setDueDate(Instant.now().plus(7, ChronoUnit.DAYS));
        ticketRepository.save(ticket1);

        Comment comment1 = new Comment();
        comment1.setTicket(ticket1);
        comment1.setAuthor(user1);
        comment1.setBody("Test Comment");
        commentRepository.save(comment1);


        entityManager.flush();
        entityManager.clear();

        User retrievedUser1 = userRepository.findById(user1.getId()).orElseThrow();
        assertThat(retrievedUser1.getEmail()).isEqualTo("emicuciu@gmail.com");
        User retrievedUser2 = userRepository.findByFullName(user2.getFullName()).orElseThrow();
        assertThat(retrievedUser2.getFullName()).isEqualTo("Nastase Varvara");

        Pageable usersPageRequest = PageRequest.of(0, 10);
        Page<User> usersWithoutPassword = userRepository.findAllByPasswordIsNull(usersPageRequest);

        List<User> usersList = usersWithoutPassword.getContent();
        assertThat(usersList).hasSize(1);


        Project retrievedProject1 = projectRepository.findProjectByName("Project1");
        assertThat(retrievedProject1.getCreatedBy()).isEqualTo(retrievedUser1);


        Pageable ticketsPageRequest = PageRequest.of(0, 10);
        Page<Ticket> retrievedTickets = ticketRepository.findAllByProjectId(retrievedProject1.getId(), ticketsPageRequest);

        Ticket ticketFromPage = retrievedTickets.getContent().getFirst();
        assertThat(ticketFromPage.getId()).isEqualTo(ticket1.getId());

        List<Comment> comments = ticketFromPage.getComments();
        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getBody()).isEqualTo("Test Comment");
        assertThat(comments.getFirst().getAuthor().getFullName()).isEqualTo("Cuciurean Emilian");


        // now we delete the project and test the cascade

        projectRepository.deleteById(retrievedProject1.getId());

        entityManager.flush();
        entityManager.clear();

        assertThat(projectRepository.findById(retrievedProject1.getId())).isEmpty();

        assertThat(ticketRepository.findById(ticketFromPage.getId())).isEmpty();

        assertThat(commentRepository.findById(comment1.getId())).isEmpty();

        assertThat(userRepository.findById(retrievedUser1.getId())).isPresent();
        assertThat(userRepository.findById(retrievedUser2.getId())).isPresent();
    }

    @Test
    void bidirectionalRelationship() {
        User user2 = new User();
        user2.setEmail("varvara@gmail.com");
        user2.setFullName("Nastase Varvara");
        user2.setRole(Role.MEMBER);
        user2.setAuthProvider(AuthProvider.GOOGLE);
        userRepository.save(user2);

        Project project1 = new Project();
        project1.setName("Project1");
        project1.setDescription("description1");
        project1.setCreatedBy(user2);
        projectRepository.save(project1);

        Ticket ticket1 = new Ticket();
        ticket1.setTitle("Ticket1");
        ticket1.setStatus(Status.IN_PROGRESS);
        ticket1.setPriority(Priority.HIGH);
        ticket1.setAssignee(user2);
        ticket1.setDueDate(Instant.now().plus(7, ChronoUnit.DAYS));
        project1.addTicket(ticket1);
        ticketRepository.save(ticket1);

        entityManager.flush();
        entityManager.clear();

        Project reloaded = projectRepository.findById(project1.getId()).orElseThrow();
        assertThat(reloaded.getTickets()).hasSize(1);
        assertThat(reloaded.getTickets().getFirst().getTitle()).isEqualTo("Ticket1");

        reloaded.getTickets().clear();
        entityManager.flush();
        entityManager.clear();

        assertThat(ticketRepository.findById(ticket1.getId())).isEmpty();
    }
}
