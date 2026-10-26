CREATE TABLE stripe_webhook_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    lease_until TIMESTAMP WITH TIME ZONE,
    processed_at TIMESTAMP WITH TIME ZONE,
    last_error_code VARCHAR(120),

    CONSTRAINT chk_stripe_webhook_status
        CHECK (status IN (
            'PENDING',
            'PROCESSING',
            'PROCESSED',
            'FAILED'
        )),

    CONSTRAINT chk_stripe_webhook_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX idx_stripe_webhook_pending
    ON stripe_webhook_events (next_attempt_at, received_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_stripe_webhook_processing
    ON stripe_webhook_events (lease_until)
    WHERE status = 'PROCESSING';