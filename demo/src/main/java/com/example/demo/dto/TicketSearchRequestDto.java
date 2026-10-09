package com.example.demo.dto;

import com.example.demo.domain.Priority;
import com.example.demo.domain.Status;

import java.time.Instant;
import java.util.UUID;

public record TicketSearchRequestDto(
        Status status,
        Priority priority,
        UUID assigneeId,
        Instant dueAfter,
        Instant dueBefore,
        String titleContains
) {
}