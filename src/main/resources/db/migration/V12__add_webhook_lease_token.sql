ALTER TABLE stripe_webhook_events
    ADD COLUMN lease_token UUID;