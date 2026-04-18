-- V1__init_schema.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
                       id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email       VARCHAR(255) UNIQUE NOT NULL,
                       password    VARCHAR(255) NOT NULL,
                       full_name   VARCHAR(255) NOT NULL,
                       role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
                       active      BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
                       updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE tickets (
                         id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         title        VARCHAR(500) NOT NULL,
                         description  TEXT,
                         status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
                         priority     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
                         category     VARCHAR(100),
                         ai_summary   TEXT,
                         created_by   UUID REFERENCES users(id),
                         assigned_to  UUID REFERENCES users(id),
                         created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE comments (
                          id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          ticket_id    UUID NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
                          author_id    UUID NOT NULL REFERENCES users(id),
                          content      TEXT NOT NULL,
                          ai_generated BOOLEAN NOT NULL DEFAULT FALSE,
                          created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tickets_status      ON tickets(status);
CREATE INDEX idx_tickets_assigned_to ON tickets(assigned_to);
CREATE INDEX idx_tickets_created_by  ON tickets(created_by);
CREATE INDEX idx_comments_ticket_id  ON comments(ticket_id);