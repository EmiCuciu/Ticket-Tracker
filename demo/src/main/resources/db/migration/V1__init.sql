CREATE TABLE users
(
    id            UUID PRIMARY KEY             DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    full_name     VARCHAR(255)        NOT NULL,
    role          VARCHAR(50)         NOT NULL,
    auth_provider VARCHAR(50)         NOT NULL,
    created_at    TIMESTAMPTZ         NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_by  UUID         NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tickets
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    title       VARCHAR(100) NOT NULL,
    description TEXT,
    status      VARCHAR(50)  NOT NULL,
    priority    VARCHAR(50)  NOT NULL,
    project     UUID         NOT NULL REFERENCES projects (id),
    assignee    UUID REFERENCES users (id),
    due_date    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE comments
(
    id         UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    ticket     UUID        NOT NULL REFERENCES tickets (id),
    author     UUID        NOT NULL REFERENCES users (id),
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tickets_status ON tickets (status);
CREATE INDEX idx_tickets_assignee_id ON tickets (assignee);
CREATE INDEX idx_tickets_due_date ON tickets (due_date);