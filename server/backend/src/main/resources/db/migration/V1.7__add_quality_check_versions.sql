-- Create the quality_check_version table
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

-- Backfill: create an initial version (v1) for every existing quality check.
-- Historical queries are not stored on the quality check, so the query is left empty and the
-- version reuses the quality check's existing hash to preserve the previous hash linkage.
INSERT INTO quality_check_version (quality_check_id, version, query, hash)
SELECT id, 1, '', hash
FROM quality_check;
