CREATE TABLE quality_check_version
(
    id               BIGSERIAL PRIMARY KEY,
    quality_check_id BIGINT  NOT NULL,
    version          INTEGER NOT NULL,
    query            TEXT    NOT NULL,
    hash             TEXT    NOT NULL,
    CONSTRAINT fk_version_quality_check
        FOREIGN KEY (quality_check_id) REFERENCES quality_check (id) ON DELETE CASCADE,
    CONSTRAINT uq_quality_check_version UNIQUE (quality_check_id, version)
);

CREATE INDEX idx_quality_check_version_quality_check_id
    ON quality_check_version (quality_check_id);

INSERT INTO quality_check_version (quality_check_id, version, query, hash)
SELECT id, 1, '', hash
FROM quality_check;

ALTER TABLE quality_check_result
    ADD COLUMN version_id BIGINT;

UPDATE quality_check_result r
SET version_id = v.id
FROM quality_check_version v
WHERE v.quality_check_id = r.quality_check_id
  AND v.version = 1;

ALTER TABLE quality_check_result
    ALTER COLUMN version_id SET NOT NULL;

-- Drop any existing primary key (V1.6 dropped the hash-based PK together with its column,
-- so on most databases none remains) before adding the version-extended PK.
DO
$$
    DECLARE
        pk_name TEXT;
    BEGIN
        SELECT conname
        INTO pk_name
        FROM pg_constraint
        WHERE conrelid = 'quality_check_result'::regclass
          AND contype = 'p'
        LIMIT 1;
        IF pk_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE quality_check_result DROP CONSTRAINT %I', pk_name);
        END IF;
    END
$$;

ALTER TABLE quality_check_result
    ADD PRIMARY KEY (report_id, quality_check_id, version_id);

ALTER TABLE quality_check_result
    ADD CONSTRAINT fk_result_version
        FOREIGN KEY (version_id) REFERENCES quality_check_version (id) ON DELETE CASCADE;

ALTER TABLE quality_check_version
    ADD CONSTRAINT uq_quality_check_version_hash UNIQUE (hash);
ALTER TABLE quality_check
    DROP CONSTRAINT IF EXISTS uq_quality_check_hash;
ALTER TABLE quality_check
    DROP COLUMN hash;