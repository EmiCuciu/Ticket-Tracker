package com.example.demo.service;

import com.example.demo.model.CommentCreateRequest;
import com.example.demo.model.CommentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {

    /**
     * Retrieves a paginated list of comments for a specific ticket.
     *
     * @param ticketId the ID of the ticket
     * @param pageable pagination information
     * @return a page of CommentDto objects
     */
    Page<CommentDto> getComments(UUID ticketId, Pageable pageable);

    /**
     * Creates a new comment for a specific ticket.
     *
     * @param ticketId the ID of the ticket
     * @param request  the request containing comment details
     * @return the created CommentDto object
     */
    CommentDto createComment(UUID ticketId, CommentCreateRequest request);
}