package com.example.demo.service.implementation;

import com.example.demo.domain.Project;
import com.example.demo.domain.Ticket;
import com.example.demo.domain.User;
import com.example.demo.dto.TicketSearchRequestDto;
import com.example.demo.exception.ProjectNotFoundException;
import com.example.demo.exception.TicketNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.mapper.JsonNullableMapper;
import com.example.demo.mapper.TicketMapper;
import com.example.demo.model.*;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.spec.TicketSpecification;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.service.TicketService;
import com.example.demo.web.TicketPageableResolver;
import com.example.demo.web.TicketSearchValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketMapper ticketMapper;
    private final TicketSearchValidator searchValidator;
    private final TicketPageableResolver pageableResolver;
    private final JsonNullableMapper jsonNullableMapper;
    private final CurrentUserProvider currentUserProvider;
    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "tickets", key = "#id")
    public TicketDto getTicket(UUID id) {
        log.info("CACHE MISS -> loading ticket {} from DB", id);
        return ticketMapper.toDto(findTicketOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketDto> searchTickets(Status status, Priority priority, UUID assigneeId,
                                         Instant dueBefore, Instant dueAfter, String titleContains,
                                         Integer page, Integer size, String sortBy, String sortDirection) {

        TicketSearchRequestDto request = new TicketSearchRequestDto(
                ticketMapper.toStatus(status),
                ticketMapper.toPriority(priority),
                assigneeId, dueAfter, dueBefore, titleContains
        );

        searchValidator.validate(request);

        String effectiveSortDirection = sortDirection;
        if (effectiveSortDirection == null || effectiveSortDirection.isBlank()) {
            effectiveSortDirection = "priority".equals(sortBy) ? "desc" : "asc";
        }

        Pageable pageable = pageableResolver.resolve(page, size, sortBy, effectiveSortDirection);
        Specification<Ticket> spec = TicketSpecification.build(request);

        if ("priority".equals(sortBy)) {
            spec = spec.and(TicketSpecification.orderByPriority(pageableResolver.resolveDirection(effectiveSortDirection)));
        }

        Page<Ticket> result = ticketRepository.findAll(spec, pageable);

        return result.map(ticketMapper::toDto);
    }

    @Override
    @Transactional
    public TicketDto createTicket(TicketCreateRequest request) {
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException(request.getProjectId()));

        User assignee = resolveAssignee(jsonNullableMapper.unwrap(request.getAssigneeId()));

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setProject(project);
        ticket.setAssignee(assignee);
        ticket.setDueDate(jsonNullableMapper.unwrap(request.getDueDate()));

        return ticketMapper.toDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    @CachePut(value = "tickets", key = "#id")
    public TicketDto updateTicket(UUID id, TicketPutRequest request) {
        Ticket ticket = findTicketOrThrow(id);
        ticketMapper.updateFromRequest(request, ticket);
        ticket.setAssignee(resolveAssignee(jsonNullableMapper.unwrap(request.getAssigneeId())));
        return ticketMapper.toDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    @CachePut(value = "tickets", key = "#id")
    public TicketDto patchTicket(UUID id, TicketPatchRequest request) {
        Ticket ticket = findTicketOrThrow(id);
        checkCanPatch(ticket);

        ticketMapper.patchFromRequest(request, ticket);

        if (jsonNullableMapper.isPresent(request.getAssigneeId())) {
            ticket.setAssignee(resolveAssignee(jsonNullableMapper.unwrap(request.getAssigneeId())));
        }

        return ticketMapper.toDto(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    @CacheEvict(value = "tickets", key = "#id")
    public void deleteTicket(UUID id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException(id);
        }
        ticketRepository.deleteById(id);
    }

    private Ticket findTicketOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    private User resolveAssignee(UUID assigneeId) {
        if (assigneeId == null)
            return null;
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new UserNotFoundException(assigneeId));
    }

    private void checkCanPatch(Ticket ticket) {
        if (currentUserProvider.hasAnyRole("ADMIN", "PROJECT_MANAGER")) {
            return;
        }
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        if (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}