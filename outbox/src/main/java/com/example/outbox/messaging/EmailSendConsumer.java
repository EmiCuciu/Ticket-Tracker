package com.example.outbox.messaging;

import com.example.outbox.config.RabbitMQConfig;
import com.example.outbox.domain.Email;
import com.example.outbox.domain.EmailStatus;
import com.example.outbox.repository.EmailRepository;
import com.example.outbox.service.MailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EmailSendConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailSendConsumer.class);
    private static final int MAX_ATTEMPTS = 5;

    private final EmailRepository emailRepository;
    private final MailService mailService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    @Transactional
    public void handle(EmailMessage message) {
        Email email = emailRepository.findById(message.emailId())
                .orElseThrow(() -> new IllegalStateException("Email not found: " + message.emailId()));

        if (email.getStatus() == EmailStatus.SENT || email.getStatus() == EmailStatus.FAILED) {
            return;
        }

        try {
            mailService.send(email);
            email.markSent();
            emailRepository.save(email);
        } catch (MailException ex) {
            email.markFailed(ex.getMessage(), MAX_ATTEMPTS);
            emailRepository.save(email);
            log.warn("Email {} failed: {}", email.getId(), ex.getMessage());
            throw ex;
        }
    }
}