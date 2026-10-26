CREATE TABLE consumer_inbox (
    consumer_name VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (consumer_name, event_id)
);

CREATE TABLE booking_confirmation_summaries (
    booking_id BIGINT PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,

    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,

    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    rooms_count INTEGER NOT NULL,

    total_price NUMERIC(14, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,

    confirmed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE
        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_confirmation_dates
        CHECK (check_out_date > check_in_date),

    CONSTRAINT chk_confirmation_rooms
        CHECK (rooms_count > 0),

    CONSTRAINT chk_confirmation_price
        CHECK (total_price > 0)
);