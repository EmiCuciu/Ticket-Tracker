package com.example.demo.repository;

import com.example.demo.domain.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findAllByTicketId(UUID ticketId, Pageable pageable);

    Page<Comment> findAllByAuthorId(UUID authorId, Pageable pageable);
}
