-- Associate quality check results with the specific version that produced them.
-- This lets a report result identify the quality check via its version hash.

ALTER TABLE quality_check_result ADD COLUMN version_id BIGINT;

-- Backfill: link each existing result to the first version (v1) of its quality check.
-- V1.7 created the v1 version for every pre-existing quality check.
UPDATE quality_check_result r
SET version_id = v.id
FROM quality_check_version v
WHERE v.quality_check_id = r.quality_check_id
  AND v.version = 1;

ALTER TABLE quality_check_result ALTER COLUMN version_id SET NOT NULL;

-- Drop any existing primary key (V1.6 dropped the hash-based PK together with its column,
-- so on most databases none remains) before adding the version-extended PK.
DO $$
DECLARE
    pk_name TEXT;
BEGIN
    SELECT conname INTO pk_name
    FROM pg_constraint
    WHERE conrelid = 'quality_check_result'::regclass AND contype = 'p'
    LIMIT 1;
    IF pk_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE quality_check_result DROP CONSTRAINT %I', pk_name);
    END IF;
END $$;

ALTER TABLE quality_check_result ADD PRIMARY KEY (report_id, quality_check_id, version_id);

ALTER TABLE quality_check_result
    ADD CONSTRAINT fk_result_version
        FOREIGN KEY (version_id) REFERENCES quality_check_version (id) ON DELETE CASCADE;
