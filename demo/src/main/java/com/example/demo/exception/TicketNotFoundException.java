package com.example.demo.exception;

public class TicketNotFoundException extends NotFoundException {

    public TicketNotFoundException(Object resourceId) {
        super("Ticket", resourceId);
    }
}
