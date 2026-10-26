CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    event_key VARCHAR(160) NOT NULL,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    lease_token UUID,
    lease_until TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    last_error_code VARCHAR(120),

    CONSTRAINT uk_outbox_event_key
        UNIQUE (event_key),

    CONSTRAINT chk_outbox_attempts
        CHECK (attempts >= 0),

    CONSTRAINT chk_outbox_status
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'PUBLISHED',
            'FAILED'
        ))
);

CREATE INDEX idx_outbox_pending
    ON outbox_events (next_attempt_at, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_processing
    ON outbox_events (lease_until)
    WHERE status = 'PROCESSING';