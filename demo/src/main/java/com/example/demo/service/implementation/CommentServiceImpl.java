package com.example.demo.service.implementation;

import com.example.demo.domain.Comment;
import com.example.demo.domain.Ticket;
import com.example.demo.domain.User;
import com.example.demo.exception.TicketNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.model.CommentCreateRequest;
import com.example.demo.model.CommentDto;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.CurrentUserProvider;
import com.example.demo.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public Page<CommentDto> getComments(UUID ticketId, Pageable pageable) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }

        Page<Comment> result = commentRepository.findAllByTicketId(ticketId, pageable);
        return result.map(commentMapper::toDto);
    }

    @Override
    @Transactional
    public CommentDto createComment(UUID ticketId, CommentCreateRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        User author = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException(currentUserId));

        Comment comment = commentMapper.toEntity(request);
        comment.setTicket(ticket);
        comment.setAuthor(author);

        return commentMapper.toDto(commentRepository.save(comment));
    }
}