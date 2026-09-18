-- Add optional public key used to verify signatures published by the central server
ALTER TABLE server
    ADD COLUMN public_key VARCHAR(2048);
