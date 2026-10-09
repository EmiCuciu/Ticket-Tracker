package com.example.demo.mapper;

import com.example.demo.domain.Comment;
import com.example.demo.model.CommentCreateRequest;
import com.example.demo.model.CommentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "ticketId", expression = "java(comment.getTicket().getId())")
    CommentDto toDto(Comment comment);

    Comment toEntity(CommentCreateRequest request);
}