-- Registration retry state: number of automatic sweep rounds used and the next scheduled retry
ALTER TABLE server
    ADD COLUMN registration_retry_count INTEGER;
ALTER TABLE server
    ADD COLUMN next_registration_retry_at TIMESTAMP;
