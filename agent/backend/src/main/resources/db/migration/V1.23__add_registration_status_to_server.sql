-- Registration status: first-class lifecycle state of the registration with the central server.
-- registered_at stays NULL for pre-existing rows (the original registration date is unknown).
ALTER TABLE server
    ADD COLUMN registration_status VARCHAR(32) NOT NULL DEFAULT 'UNREGISTERED';
ALTER TABLE server
    ADD COLUMN registration_error_reason VARCHAR(64);
ALTER TABLE server
    ADD COLUMN registration_error_message TEXT;
ALTER TABLE server
    ADD COLUMN registration_registered_at TIMESTAMP;
UPDATE server
SET registration_status = CASE
    WHEN client_id <> '' THEN 'REGISTERED_PENDING_APPROVAL'
    WHEN status = 'ERROR' THEN 'FAILED'
    ELSE 'UNREGISTERED'
END;
