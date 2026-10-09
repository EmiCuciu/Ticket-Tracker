package com.example.demo.controller;

import com.example.demo.api.TicketsApi;
import com.example.demo.model.*;
import com.example.demo.service.TicketService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
public class TicketController implements TicketsApi {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public ResponseEntity<TicketPage> getTickets(Status status, Priority priority, UUID assigneeId,
                                                 Instant dueBefore, Instant dueAfter, String titleContains,
                                                 Integer page, Integer size, String sortBy, String sortDirection) {

        Page<TicketDto> result = ticketService.searchTickets(status, priority, assigneeId,
                dueBefore, dueAfter, titleContains, page, size, sortBy, sortDirection);

        TicketPage response = new TicketPage();
        response.setContent(result.getContent());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TicketDto> getTicketById(UUID id) {
        return ResponseEntity.ok(ticketService.getTicket(id));
    }

    @Override
    public ResponseEntity<TicketDto> createTicket(TicketCreateRequest ticketCreateRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(ticketCreateRequest));
    }

    @Override
    public ResponseEntity<TicketDto> updateTicket(UUID id, TicketPutRequest ticketUpdateRequest) {
        return ResponseEntity.ok(ticketService.updateTicket(id, ticketUpdateRequest));
    }

    @Override
    public ResponseEntity<TicketDto> patchTicket(UUID id, TicketPatchRequest ticketUpdateRequest) {
        return ResponseEntity.ok(ticketService.patchTicket(id, ticketUpdateRequest));
    }

    @Override
    public ResponseEntity<Void> deleteTicket(UUID id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}