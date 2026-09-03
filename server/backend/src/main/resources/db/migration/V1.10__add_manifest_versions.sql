CREATE TABLE manifest_version
(
    id           BIGSERIAL PRIMARY KEY,
    manifest_id  BIGINT NOT NULL,
    version      INTEGER                  NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    body         TEXT                     NOT NULL,
    signature    TEXT,
    key_id       VARCHAR(255),
    CONSTRAINT uq_manifest_version UNIQUE (manifest_id, version)
);

CREATE INDEX idx_manifest_version_manifest_id
    ON manifest_version (manifest_id);

INSERT INTO manifest_version (manifest_id, version, generated_at, body, signature, key_id)
SELECT id, 1, generated_at, body, signature, key_id
FROM manifest;

ALTER TABLE manifest_version
    ADD CONSTRAINT fk_manifest_version_manifest
        FOREIGN KEY (manifest_id) REFERENCES manifest (id) ON DELETE CASCADE;

ALTER TABLE manifest
    DROP COLUMN generated_at,
    DROP COLUMN body,
    DROP COLUMN signature,
    DROP COLUMN key_id;
