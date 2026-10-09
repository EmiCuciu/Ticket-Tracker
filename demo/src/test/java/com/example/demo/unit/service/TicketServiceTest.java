package com.example.demo.unit.service;

import com.example.demo.domain.Project;
import com.example.demo.domain.Ticket;
import com.example.demo.domain.User;
import com.example.demo.dto.TicketSearchRequestDto;
import com.example.demo.exception.ProjectNotFoundException;
import com.example.demo.exception.TicketNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.mapper.JsonNullableMapper;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.model.TicketCreateRequest;
import com.example.demo.model.TicketDto;
import com.example.demo.model.TicketPatchRequest;
import com.example.demo.model.TicketPutRequest;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.service.implementation.TicketServiceImpl;
import com.example.demo.web.TicketPageableResolver;
import com.example.demo.web.TicketSearchValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TicketMapper ticketMapper;
    @Mock
    private TicketSearchValidator searchValidator;
    @Mock
    private TicketPageableResolver pageableResolver;
    @Mock
    private JsonNullableMapper jsonNullableMapper;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @InjectMocks
    private TicketServiceImpl ticketService;

    private UUID ticketId, projectId;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        Project project = new Project();
        project.setId(projectId);
        ticket = new Ticket();
        ticket.setId(ticketId);
    }

    @Test
    void searchTicketsTest() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(ticket));

        when(pageableResolver.resolve(0, 10, "priority", "desc")).thenReturn(pageable);
        when(pageableResolver.resolveDirection("desc")).thenReturn(Sort.Direction.DESC);
        when(ticketRepository.findAll(isA(Specification.class), eq(pageable))).thenReturn(page);
        when(ticketMapper.toDto(ticket)).thenReturn(new TicketDto());

        Page<TicketDto> result = ticketService.searchTickets(
                null, null, null, null, null, null,
                0, 10, "priority", null);

        assertThat(result.getContent()).hasSize(1);

        verify(ticketMapper).toStatus(null);
        verify(ticketMapper).toPriority(null);
        verify(searchValidator).validate(isA(TicketSearchRequestDto.class));
        verify(pageableResolver).resolve(0, 10, "priority", "desc");
        verify(pageableResolver).resolveDirection("desc");
        verify(ticketRepository).findAll(isA(Specification.class), eq(pageable));
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(searchValidator, pageableResolver, ticketRepository, ticketMapper);
    }

    @Test
    void getTicket_notFound_throws() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(TicketNotFoundException.class)
                .isThrownBy(() -> ticketService.getTicket(ticketId));

        verify(ticketRepository).findById(ticketId);
        verifyNoMoreInteractions(ticketRepository);
    }

    @Test
    void getTicketTest() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        TicketDto ticketDto = new TicketDto();
        when(ticketMapper.toDto(ticket)).thenReturn(ticketDto);

        assertThat(ticketService.getTicket(ticketId)).isEqualTo(ticketDto);

        verify(ticketRepository).findById(ticketId);
        verify(ticketMapper).toDto(ticket);
        verifyNoMoreInteractions(ticketRepository, ticketMapper);
    }

    @Test
    void createTicket_projectNotFound_throws() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setProjectId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ProjectNotFoundException.class)
                .isThrownBy(() -> ticketService.createTicket(request));

        verify(projectRepository).findById(projectId);
        verifyNoInteractions(ticketRepository, userRepository, ticketMapper);
        verifyNoMoreInteractions(projectRepository);
    }

    @Test
    void createTicket_assigneeNotFound_throws() {
        Project project = new Project();
        project.setId(projectId);
        UUID assigneeId = UUID.randomUUID();

        TicketCreateRequest request = new TicketCreateRequest();
        request.setProjectId(projectId);
        request.setAssigneeId(JsonNullable.of(assigneeId));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.empty());
        when(jsonNullableMapper.unwrap(request.getAssigneeId())).thenReturn(assigneeId);

        assertThatExceptionOfType(UserNotFoundException.class)
                .isThrownBy(() -> ticketService.createTicket(request));

        verify(projectRepository).findById(projectId);
        verify(userRepository).findById(assigneeId);
        verifyNoInteractions(ticketRepository, ticketMapper);
        verifyNoMoreInteractions(projectRepository, userRepository);
    }

    @Test
    void createTicketTest() {
        UUID assigneeId = UUID.randomUUID();
        User assignee = new User();
        assignee.setId(assigneeId);

        Project project = new Project();
        project.setId(projectId);

        TicketCreateRequest request = new TicketCreateRequest();
        request.setProjectId(projectId);
        request.setAssigneeId(JsonNullable.of(assigneeId));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(ticketMapper.toEntity(request)).thenReturn(ticket);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(new TicketDto());
        when(jsonNullableMapper.unwrap(request.getAssigneeId())).thenReturn(assigneeId);

        ticketService.createTicket(request);

        assertThat(ticket.getProject()).isEqualTo(project);
        assertThat(ticket.getAssignee()).isEqualTo(assignee);

        verify(projectRepository).findById(projectId);
        verify(userRepository).findById(assigneeId);
        verify(ticketMapper).toEntity(request);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);
        verifyNoMoreInteractions(projectRepository, userRepository, ticketMapper, ticketRepository);
    }

    @Test
    void deleteTicket_notFound_throws() {
        when(ticketRepository.existsById(ticketId)).thenReturn(false);

        assertThatExceptionOfType(TicketNotFoundException.class)
                .isThrownBy(() -> ticketService.deleteTicket(ticketId));

        verify(ticketRepository).existsById(ticketId);
        verifyNoMoreInteractions(ticketRepository);
    }

    @Test
    void deleteTicketTest() {
        when(ticketRepository.existsById(ticketId)).thenReturn(true);

        ticketService.deleteTicket(ticketId);

        verify(ticketRepository).existsById(ticketId);
        verify(ticketRepository).deleteById(ticketId);
        verifyNoMoreInteractions(ticketRepository);
    }

    @Test
    void updateTicketTest_overwritesAndClearsAssignee() {
        TicketPutRequest request = new TicketPutRequest();
        request.setAssigneeId(null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(new TicketDto());

        ticketService.updateTicket(ticketId, request);

        assertThat(ticket.getAssignee()).isNull();

        verify(ticketRepository).findById(ticketId);
        verify(ticketMapper).updateFromRequest(request, ticket);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);

        verifyNoInteractions(userRepository);
        verifyNoMoreInteractions(ticketRepository, ticketMapper);
    }

    @Test
    void patchTicketTest_absentAssigneeId_doesntChangeAssignee() {
        User assignee = new User();
        ticket.setAssignee(assignee);
        TicketPatchRequest request = new TicketPatchRequest();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(currentUserProvider.hasAnyRole("ADMIN", "PROJECT_MANAGER")).thenReturn(true);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(new TicketDto());

        ticketService.patchTicket(ticketId, request);

        assertThat(ticket.getAssignee()).isEqualTo(assignee);

        verify(ticketRepository).findById(ticketId);
        verify(ticketMapper).patchFromRequest(request, ticket);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);

        verifyNoInteractions(userRepository);
        verifyNoMoreInteractions(ticketRepository, ticketMapper);
    }

    @Test
    void patchTicketTest_withAssigneeId_resolvesNewAssignee() {
        UUID assigneeId = UUID.randomUUID();
        User assignee = new User();
        assignee.setId(assigneeId);

        TicketPatchRequest request = new TicketPatchRequest();
        request.setAssigneeId(JsonNullable.of(assigneeId));

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(currentUserProvider.hasAnyRole("ADMIN", "PROJECT_MANAGER")).thenReturn(true);
        when(jsonNullableMapper.isPresent(request.getAssigneeId())).thenReturn(true);
        when(jsonNullableMapper.unwrap(request.getAssigneeId())).thenReturn(assigneeId);
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(new TicketDto());

        ticketService.patchTicket(ticketId, request);

        assertThat(ticket.getAssignee()).isEqualTo(assignee);

        verify(ticketRepository).findById(ticketId);
        verify(ticketMapper).patchFromRequest(request, ticket);
        verify(jsonNullableMapper).isPresent(request.getAssigneeId());
        verify(jsonNullableMapper).unwrap(request.getAssigneeId());
        verify(userRepository).findById(assigneeId);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(ticketRepository, userRepository, ticketMapper);
    }

    @Test
    void updateTicket_notFound_throws() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(TicketNotFoundException.class)
                .isThrownBy(() -> ticketService.updateTicket(ticketId, new TicketPutRequest()));

        verify(ticketRepository).findById(ticketId);
        verifyNoMoreInteractions(ticketRepository);
        verifyNoInteractions(ticketMapper, userRepository);
    }

    @Test
    void patchTicket_notFound_throws() {
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatExceptionOfType(TicketNotFoundException.class)
                .isThrownBy(() -> ticketService.patchTicket(ticketId, new TicketPatchRequest()));

        verify(ticketRepository).findById(ticketId);
        verifyNoMoreInteractions(ticketRepository);
        verifyNoInteractions(ticketMapper, userRepository);
    }

    @Test
    void searchTicketsTest_withFilters() {
        UUID assigneeId = UUID.randomUUID();
        Instant dueBefore = Instant.now().plus(5, ChronoUnit.DAYS);
        Instant dueAfter = Instant.now();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Ticket> page = new PageImpl<>(List.of(ticket));

        when(ticketMapper.toStatus(com.example.demo.model.Status.OPEN)).thenReturn(com.example.demo.domain.Status.OPEN);
        when(ticketMapper.toPriority(com.example.demo.model.Priority.HIGH)).thenReturn(com.example.demo.domain.Priority.HIGH);
        when(pageableResolver.resolve(0, 10, null, "asc")).thenReturn(pageable);
        when(ticketRepository.findAll(isA(Specification.class), eq(pageable))).thenReturn(page);
        when(ticketMapper.toDto(ticket)).thenReturn(new TicketDto());

        Page<TicketDto> result = ticketService.searchTickets(
                com.example.demo.model.Status.OPEN, com.example.demo.model.Priority.HIGH, assigneeId,
                dueBefore, dueAfter, "bug",
                0, 10, null, null);

        assertThat(result.getContent()).hasSize(1);

        verify(ticketMapper).toStatus(com.example.demo.model.Status.OPEN);
        verify(ticketMapper).toPriority(com.example.demo.model.Priority.HIGH);
        verify(pageableResolver).resolve(0, 10, null, "asc");
        verify(searchValidator).validate(isA(TicketSearchRequestDto.class));
        verify(ticketRepository).findAll(isA(Specification.class), eq(pageable));
        verify(ticketMapper).toDto(ticket);

        verifyNoMoreInteractions(searchValidator, pageableResolver, ticketRepository, ticketMapper);
    }

    @Test
    void patchTicket_nonAssigneeMember_throwsAccessDenied() {
        User assignee = new User();
        assignee.setId(UUID.randomUUID());
        ticket.setAssignee(assignee);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(currentUserProvider.hasAnyRole("ADMIN", "PROJECT_MANAGER")).thenReturn(false);
        when(currentUserProvider.getCurrentUserId()).thenReturn(UUID.randomUUID());

        assertThatExceptionOfType(AccessDeniedException.class)
                .isThrownBy(() -> ticketService.patchTicket(ticketId, new TicketPatchRequest()));

        verify(ticketRepository, never()).save(any());
        verifyNoInteractions(ticketMapper);
    }

    @Test
    void patchTicket_assigneeMember_allowed() {
        UUID userId = UUID.randomUUID();
        User assignee = new User();
        assignee.setId(userId);
        ticket.setAssignee(assignee);
        TicketPatchRequest request = new TicketPatchRequest();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(currentUserProvider.hasAnyRole("ADMIN", "PROJECT_MANAGER")).thenReturn(false);
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toDto(ticket)).thenReturn(new TicketDto());

        ticketService.patchTicket(ticketId, request);

        verify(ticketMapper).patchFromRequest(request, ticket);
    }
}