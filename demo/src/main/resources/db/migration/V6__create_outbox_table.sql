TRUNCATE TABLE emails;

ALTER TABLE emails
    DROP COLUMN next_attempt_at;

CREATE TABLE outbox_events
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50) NOT NULL,
    aggregate_id    UUID        NOT NULL,
    event_type      VARCHAR(50) NOT NULL,
    payload         TEXT        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_outbox_events_next_attempt ON outbox_events (next_attempt_at);