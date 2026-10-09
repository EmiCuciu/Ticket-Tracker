package com.example.demo.unit.mapper;

import com.example.demo.domain.*;
import com.example.demo.mapper.JsonNullableMapperImpl;
import com.example.demo.mapper.TicketMapperImpl;
import com.example.demo.model.TicketDto;
import com.example.demo.model.TicketPatchRequest;
import com.example.demo.model.TicketPutRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.context.annotation.Profile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Profile("test")
public class TicketMapperTest {

    private TicketMapperImpl mapper;

    @BeforeEach
    void setUp() {
        mapper = new TicketMapperImpl();
        ReflectionTestUtils.setField(mapper, "jsonNullableMapper", new JsonNullableMapperImpl());
    }

    @Test
    void toDto_mapsNestedAssigneeAndProject() {
        User assignee = new User("a@test.com", "p", "Name" , Role.MEMBER, AuthProvider.LOCAL);
        ReflectionTestUtils.setField(assignee, "id", UUID.randomUUID());

        Project project = new Project("Project", "description");
        ReflectionTestUtils.setField(project, "id", UUID.randomUUID());

        Ticket ticket = new Ticket("Title", "desc", Status.OPEN, Priority.HIGH, project, assignee, Instant.now());
        ReflectionTestUtils.setField(ticket, "id", UUID.randomUUID());

        TicketDto dto = mapper.toDto(ticket);

        assertThat(dto.getTitle()).isEqualTo("Title");
        assertThat(dto.getAssignee().getFullName()).isEqualTo("Name");
        assertThat(dto.getProject().getName()).isEqualTo("Project");
    }

    @Test
    void toDto_nullAssignee_mapsNull() {
        Project project = new Project("Proj", "desc");
        Ticket ticket = new Ticket("Title", "desc", Status.OPEN, Priority.LOW, project, null, null);

        TicketDto dto = mapper.toDto(ticket);

        assertThat(dto.getAssignee()).isNull();
    }

    @Test
    void updateFromRequest_overwritesFields_ignoresRelations() {
        Ticket ticket = new Ticket("Old", "old desc", Status.OPEN, Priority.LOW, new Project("p", "d"), null, null);

        TicketPutRequest request = new TicketPutRequest();
        request.setTitle("New title");
        request.setDescription("New desc");
        request.setStatus(com.example.demo.model.Status.DONE);
        request.setPriority(com.example.demo.model.Priority.URGENT);

        mapper.updateFromRequest(request, ticket);

        assertThat(ticket.getTitle()).isEqualTo("New title");
        assertThat(ticket.getStatus()).isEqualTo(Status.DONE);
        assertThat(ticket.getPriority()).isEqualTo(Priority.URGENT);
    }

    @Test
    void patchFromRequest_nullFields_areIgnored() {
        Ticket ticket = new Ticket("Existing", "existing desc", Status.OPEN, Priority.LOW, new Project("p", "d"), null, null);

        TicketPatchRequest request = new TicketPatchRequest();
        request.setStatus(JsonNullable.of(com.example.demo.model.Status.DONE));

        mapper.patchFromRequest(request, ticket);

        assertThat(ticket.getStatus()).isEqualTo(Status.DONE);
        assertThat(ticket.getTitle()).isEqualTo("Existing");
    }

    @Test
    void toStatusAndPriority_mapEnumsCorrectly() {
        assertThat(mapper.toStatus(com.example.demo.model.Status.OPEN)).isEqualTo(Status.OPEN);
        assertThat(mapper.toPriority(com.example.demo.model.Priority.URGENT)).isEqualTo(Priority.URGENT);
    }
}
