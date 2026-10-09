package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emails")
@Getter
@Setter
@NoArgsConstructor
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String recipient;
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    private EmailStatus status;

    private int attempts;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    private String dedupeKey;

    private Instant createdAt;

    private Instant sentAt;

    public Email(String recipient, String subject, String body, String dedupeKey) {
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.dedupeKey = dedupeKey;
        this.status = EmailStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void markSent() {
        status = EmailStatus.SENT;
        sentAt = Instant.now();
    }

    public void markFailed(String error, int maxAttempts) {
        attempts++;
        lastError = error;
        if (attempts >= maxAttempts) {
            status = EmailStatus.FAILED;
        }
    }
}