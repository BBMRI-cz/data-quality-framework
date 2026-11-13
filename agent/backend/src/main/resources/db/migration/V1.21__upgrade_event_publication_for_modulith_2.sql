ALTER TABLE event_publication ADD COLUMN status VARCHAR(20);
ALTER TABLE event_publication ADD COLUMN completion_attempts INTEGER;
ALTER TABLE event_publication ADD COLUMN last_resubmission_date TIMESTAMP;

UPDATE event_publication
SET status = CASE WHEN completion_date IS NULL THEN 'PUBLISHED' ELSE 'COMPLETED' END,
    completion_attempts = CASE WHEN completion_date IS NULL THEN 0 ELSE 1 END
WHERE status IS NULL;
