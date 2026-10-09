package com.example.demo.config;

import com.example.demo.domain.*;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@Profile("test-seed")
public class TestDataSeeder {

    @Bean
    public CommandLineRunner seedData(UserRepository userRepository,
                                      ProjectRepository projectRepository,
                                      TicketRepository ticketRepository) {
        return args -> {
            Instant now = Instant.now();

            User admin = userRepository.findByEmail("admin@test.com")
                    .orElseGet(() -> userRepository.save(
                            new User("admin@test.com", "encoded_pass", "Admin User", Role.ADMIN, AuthProvider.LOCAL)));

            User pm = userRepository.findByEmail("pm@test.com")
                    .orElseGet(() -> userRepository.save(
                            new User("pm@test.com", "encoded_pass", "Manager User", Role.PROJECT_MANAGER, AuthProvider.LOCAL)));

            User member = userRepository.findByEmail("member@test.com")
                    .orElseGet(() -> userRepository.save(
                            new User("member@test.com", "encoded_pass", "Member User", Role.MEMBER, AuthProvider.LOCAL)));

            Project project1 = projectRepository.findProjectByName("Project Low");
            if (project1 == null) {
                project1 = new Project("Project Low", "Description Low");
                project1.setCreatedBy(pm);
                project1 = projectRepository.save(project1);
            }

            Project project2 = projectRepository.findProjectByName("Project URGENT");
            if (project2 == null) {
                project2 = new Project("Project URGENT", "Description Urgent");
                project2.setCreatedBy(admin);
                project2 = projectRepository.save(project2);
            }

            if (ticketRepository.findAllByProjectId(project1.getId(), Pageable.unpaged()).isEmpty()) {
                Ticket ticket1 = new Ticket("Ticket Low (Project Low)", "desc", Status.OPEN, Priority.LOW, project1, admin, now.plus(5, ChronoUnit.DAYS));
                Ticket ticket2 = new Ticket("Ticket HIGH (Project Low)", "desc", Status.IN_PROGRESS, Priority.HIGH, project1, admin, now.plus(5, ChronoUnit.DAYS));
                ticketRepository.saveAll(List.of(ticket1, ticket2));
            }

            if (ticketRepository.findAllByProjectId(project2.getId(), Pageable.unpaged()).isEmpty()) {
                Ticket ticket3 = new Ticket("Ticket Medium (Project URGENT)", "desc", Status.OPEN, Priority.MEDIUM, project2, pm, now.plus(2, ChronoUnit.DAYS));
                Ticket ticket4 = new Ticket("Ticket URGENT (Project URGENT)", "desc", Status.IN_PROGRESS, Priority.URGENT, project2, member, now.plus(1, ChronoUnit.DAYS));
                ticketRepository.saveAll(List.of(ticket3, ticket4));
            }
        };
    }
}