package com.example.demo.job;

import com.example.demo.domain.Ticket;
import com.example.demo.domain.User;
import com.example.demo.repository.EmailRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.spec.TicketSpecification;
import com.example.demo.service.implementation.TicketDigestEmailService;
import lombok.RequiredArgsConstructor;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
@RequiredArgsConstructor
public class TicketDigestJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(TicketDigestJob.class);

    private final TicketRepository ticketRepository;
    private final EmailRepository emailRepository;
    private final TicketDigestEmailService ticketDigestEmailService;
    private final Clock clock;

    @Override
    public void execute(JobExecutionContext context) {
        LocalDate startOfToday = LocalDate.now(clock);
        Instant threshold = startOfToday.plusDays(2).atStartOfDay(clock.getZone()).toInstant();

        String initiatorUserIdStr = context.getMergedJobDataMap().getString("initiatorUserId");
        UUID initiatorUserId = initiatorUserIdStr != null ? UUID.fromString(initiatorUserIdStr) : null;

        List<Ticket> dueTickets = ticketRepository.findAll(TicketSpecification.dueForDigest(threshold));

        Map<User, List<Ticket>> byAssignee = dueTickets.stream()
                .filter(t -> t.getAssignee() != null)
                .filter(t -> initiatorUserId == null || t.getAssignee().getId().equals(initiatorUserId))
                .collect(Collectors.groupingBy(Ticket::getAssignee));

        int notifiedCount = 0;

        for (Map.Entry<User, List<Ticket>> entry : byAssignee.entrySet()) {
            User assignee = entry.getKey();
            String dedupeKey = "ticket-digest:" + assignee.getId() + ":" + startOfToday;

            if (emailRepository.existsByDedupeKey(dedupeKey))
                continue;

            if (ticketDigestEmailService.createEmailAndOutboxEvent(assignee, entry.getValue(), dedupeKey)) {
                notifiedCount++;
            }
        }

        log.info("Ticket digest job finished, wrote {} email(s) to outbox", notifiedCount);
    }
}