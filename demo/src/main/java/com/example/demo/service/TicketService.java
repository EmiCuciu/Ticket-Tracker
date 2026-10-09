package com.example.demo.service;

import com.example.demo.model.*;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.UUID;

public interface TicketService {

    /**
     * Searches for tickets based on the provided criteria.
     * For sortBy "priority", the results will be sorted descending ( URGENT > HIGH > MEDIUM > LOW )
     *
     * @param status        the status of the ticket (optional)
     * @param priority      the priority of the ticket (optional)
     * @param assigneeId    the ID of the assignee (optional)
     * @param dueBefore     the due date before which tickets should be returned (optional)
     * @param dueAfter      the due date after which tickets should be returned (optional)
     * @param titleContains a substring that should be contained in the ticket title (optional)
     * @param page          the page number to retrieve (0-based index, optional)
     * @param size          the number of tickets per page (optional)
     * @param sortBy        the field by which to sort the results (optional)
     * @param sortDirection the direction of sorting, either "asc" or "desc" (optional)
     * @return a page of TicketDto objects matching the search criteria
     */
    Page<TicketDto> searchTickets(Status status, Priority priority, UUID assigneeId,
                                  Instant dueBefore, Instant dueAfter, String titleContains,
                                  Integer page, Integer size, String sortBy, String sortDirection);

    /**
     * Retrieves a ticket by its ID.
     *
     * @param id the ID of the ticket
     * @return the TicketDto object corresponding to the specified ID
     */
    TicketDto getTicket(UUID id);

    /**
     * Creates a new ticket with the provided details.
     *
     * @param request the request containing ticket details
     * @return the created TicketDto object
     */
    TicketDto createTicket(TicketCreateRequest request);

    /**
     * Updates an existing ticket with the specified ID.
     *
     * @param id      the ID of the ticket to update
     * @param request the request containing updated ticket details
     * @return the updated TicketDto object
     */
    TicketDto updateTicket(UUID id, TicketPutRequest request);

    /**
     * Partially updates an existing ticket with the specified ID.
     *
     * @param id      the ID of the ticket to update
     * @param request the request containing updated ticket details
     * @return the updated TicketDto object
     */
    TicketDto patchTicket(UUID id, TicketPatchRequest request);

    /**
     * Deletes a ticket with the specified ID.
     *
     * @param id the ID of the ticket to delete
     */
    void deleteTicket(UUID id);
}