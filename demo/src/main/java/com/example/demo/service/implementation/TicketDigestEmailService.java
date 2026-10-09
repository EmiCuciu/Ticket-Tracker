package com.example.demo.service.implementation;

import com.example.demo.domain.Email;
import com.example.demo.domain.OutboxEvent;
import com.example.demo.domain.Ticket;
import com.example.demo.domain.User;
import com.example.demo.repository.EmailRepository;
import com.example.demo.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketDigestEmailService {

    private final EmailRepository emailRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TemplateEngine templateEngine;

    @Transactional
    public boolean createEmailAndOutboxEvent(User assignee, List<Ticket> tickets, String dedupeKey) {
        Context ctx = new Context();
        ctx.setVariable("name", assignee.getFullName());
        ctx.setVariable("tickets", tickets);

        Email email = emailRepository.save(new Email(
                assignee.getEmail(),
                "Ticket Digest: Action Required on Your Tickets",
                templateEngine.process("ticket-digest", ctx),
                dedupeKey));

        outboxEventRepository.save(new OutboxEvent(
                "EMAIL", email.getId(), "EMAIL_CREATED", email.getId().toString()));

        return true;
    }
}