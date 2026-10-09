package com.example.demo.web;

import com.example.demo.dto.TicketSearchRequestDto;
import com.example.demo.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class TicketSearchValidator {

    public void validate(TicketSearchRequestDto request) {
        if (request.dueBefore() != null && request.dueAfter() != null
                && request.dueBefore().isBefore(request.dueAfter())) {
            throw new BadRequestException("dueBefore must not be earlier than dueAfter");
        }
    }
}