-- Add active flag to quality_check; checks are active by default and can be toggled by users
ALTER TABLE quality_check
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
