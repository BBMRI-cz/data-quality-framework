CREATE TABLE manifest_version_quality_check
(
    manifest_version_id     BIGINT NOT NULL,
    quality_check_version_id BIGINT NOT NULL,
    CONSTRAINT uq_manifest_version_quality_check UNIQUE (manifest_version_id, quality_check_version_id),
    CONSTRAINT fk_mvqc_manifest_version
        FOREIGN KEY (manifest_version_id) REFERENCES manifest_version (id) ON DELETE CASCADE,
    CONSTRAINT fk_mvqc_quality_check_version
        FOREIGN KEY (quality_check_version_id) REFERENCES quality_check_version (id) ON DELETE CASCADE
);

CREATE INDEX idx_manifest_version_quality_check_version
    ON manifest_version_quality_check (quality_check_version_id);
