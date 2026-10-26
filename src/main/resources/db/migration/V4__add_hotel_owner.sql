ALTER TABLE hotels
    ADD COLUMN owner_id BIGINT;

ALTER TABLE hotels
    ADD CONSTRAINT fk_hotels_owner
    FOREIGN KEY (owner_id) REFERENCES users(id);

CREATE INDEX idx_hotels_owner_id ON hotels (owner_id);