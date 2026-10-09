package com.example.demo.controller;

import com.example.demo.api.CommentsApi;
import com.example.demo.model.CommentCreateRequest;
import com.example.demo.model.CommentDto;
import com.example.demo.model.CommentPage;
import com.example.demo.service.CommentService;
import com.example.demo.web.EntityPageableResolver;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class CommentController implements CommentsApi {

    private final CommentService commentService;
    private final EntityPageableResolver pageableResolver;

    public CommentController(CommentService commentService, EntityPageableResolver pageableResolver) {
        this.commentService = commentService;
        this.pageableResolver = pageableResolver;
    }

    @Override
    public ResponseEntity<CommentPage> getTicketComments(UUID ticketId, Integer page, Integer size) {
        Page<CommentDto> result = commentService.getComments(ticketId, pageableResolver.resolve(page, size));
        CommentPage response = new CommentPage();
        response.setContent(result.getContent());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<CommentDto> createComment(UUID ticketId, CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(ticketId, request));
    }
}