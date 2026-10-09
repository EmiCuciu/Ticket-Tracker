CREATE TABLE emails
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    recipient       VARCHAR(255) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    attempts        INT          NOT NULL DEFAULT 0,
    last_error      TEXT,
    dedupe_key      VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL,
    next_attempt_at TIMESTAMPTZ  NOT NULL,
    sent_at         TIMESTAMPTZ
);

CREATE INDEX idx_emails_status_next_attempt ON emails (status, next_attempt_at);