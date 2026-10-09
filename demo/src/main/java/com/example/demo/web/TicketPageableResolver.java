package com.example.demo.web;

import com.example.demo.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TicketPageableResolver {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("dueDate", "priority", "createdAt");

    public Pageable resolve(Integer page, Integer size, String sortBy, String sortDirection) {
        int resolvedPage = page != null ? page : 0;
        int resolvedSize = size != null ? size : 20;

        if (resolvedPage < 0) {
            throw new BadRequestException("page must be >= 0");
        }

        return PageRequest.of(resolvedPage, resolvedSize, resolveSort(sortBy, sortDirection));
    }

    private Sort resolveSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.isBlank()) {
            return Sort.unsorted();
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new BadRequestException("Invalid sortBy '" + sortBy + "'. Allowed values: " + ALLOWED_SORT_FIELDS);
        }

        Sort.Direction direction = resolveDirection(sortDirection);

        if ("priority".equals(sortBy)) {
            return Sort.unsorted();
        }

        return Sort.by(direction, sortBy);
    }

    public Sort.Direction resolveDirection(String sortDirection) {
        try {
            return sortDirection != null ? Sort.Direction.fromString(sortDirection) : Sort.Direction.ASC;
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid sortDirection '" + sortDirection + "'. Allowed values: asc, desc");
        }
    }
}