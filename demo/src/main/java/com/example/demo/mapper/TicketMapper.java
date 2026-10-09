package com.example.demo.mapper;

import com.example.demo.domain.Priority;
import com.example.demo.domain.Status;
import com.example.demo.domain.Ticket;
import com.example.demo.model.TicketCreateRequest;
import com.example.demo.model.TicketDto;
import com.example.demo.model.TicketPatchRequest;
import com.example.demo.model.TicketPutRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = JsonNullableMapper.class)
public interface TicketMapper {
    TicketDto toDto(Ticket ticket);

    @Mapping(target = "dueDate", ignore = true)
    Ticket toEntity(TicketCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "comments", ignore = true)
    void updateFromRequest(TicketPutRequest request, @MappingTarget Ticket entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "assignee", ignore = true)
    @Mapping(target = "comments", ignore = true)
    void patchFromRequest(TicketPatchRequest request, @MappingTarget Ticket entity);

    Status toStatus(com.example.demo.model.Status status);

    Priority toPriority(com.example.demo.model.Priority priority);
}
