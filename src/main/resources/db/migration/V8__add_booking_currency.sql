ALTER TABLE bookings
    ADD COLUMN currency VARCHAR(3);

-- Learning-project assumption: existing booking prices represent USD.
UPDATE bookings
SET currency = 'USD';

ALTER TABLE bookings
    ALTER COLUMN currency SET NOT NULL;

ALTER TABLE bookings
    ADD CONSTRAINT chk_bookings_currency
    CHECK (currency = 'USD');