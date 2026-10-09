package com.example.outbox.job;

import com.example.outbox.config.RabbitMQConfig;
import com.example.outbox.domain.OutboxEvent;
import com.example.outbox.messaging.EmailMessage;
import com.example.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxRelayJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayJob.class);
    private static final int BATCH_SIZE = 20;

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelayString = "${outbox.relay.poll-interval-ms}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = outboxEventRepository.lockNextBatch(Instant.now(), PageRequest.of(0, BATCH_SIZE));
        if (batch.isEmpty()) {
            return;
        }

        List<OutboxEvent> published = new ArrayList<>();

        for (OutboxEvent event : batch) {
            try {
                UUID emailId = UUID.fromString(event.getPayload());
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY,
                        new EmailMessage(emailId));
                published.add(event);
            } catch (Exception ex) {
                log.error("Failed to publish outbox event {}, backing off", event.getId(), ex);
                event.setNextAttemptAt(Instant.now().plus(30, ChronoUnit.SECONDS));
                outboxEventRepository.save(event);
            }
        }

        outboxEventRepository.deleteAll(published);
        log.info("Outbox relay: published and removed {}/{} events", published.size(), batch.size());
    }
}