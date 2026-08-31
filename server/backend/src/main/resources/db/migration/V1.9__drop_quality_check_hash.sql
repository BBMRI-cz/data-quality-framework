ALTER TABLE quality_check DROP CONSTRAINT IF EXISTS uq_quality_check_hash;
ALTER TABLE quality_check DROP COLUMN hash;
