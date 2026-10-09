package com.example.outbox.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEvent {

    @Id
    private UUID id;

    private String aggregateType;
    private UUID aggregateId;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private Instant createdAt;
    private Instant nextAttemptAt;
}