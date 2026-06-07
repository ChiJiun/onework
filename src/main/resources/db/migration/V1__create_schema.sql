CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    location VARCHAR(200),
    equipment VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'REVIEWER', 'ADMIN')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    requester_id BIGINT NOT NULL REFERENCES app_users(id),
    reviewer_id BIGINT REFERENCES app_users(id),
    title VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    attendee_count INTEGER NOT NULL CHECK (attendee_count > 0),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING', 'REJECTED', 'APPROVED')),
    rejection_reason VARCHAR(500),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_reservation_time CHECK (end_time > start_time),
    CONSTRAINT chk_rejection_reason CHECK (
        status <> 'REJECTED' OR rejection_reason IS NOT NULL
    )
);

CREATE INDEX idx_reservation_room_time
    ON reservations(room_id, start_time, end_time);
CREATE INDEX idx_reservation_status_start
    ON reservations(status, start_time);
CREATE INDEX idx_reservation_requester
    ON reservations(requester_id);

ALTER TABLE reservations
    ADD CONSTRAINT no_overlapping_active_reservations
    EXCLUDE USING gist (
        room_id WITH =,
        tsrange(start_time, end_time, '[)') WITH &&
    )
    WHERE (status IN ('PROCESSING', 'APPROVED'));
