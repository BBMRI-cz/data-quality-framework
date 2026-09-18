-- Create manifest table for manifests pulled from central servers
CREATE TABLE manifest (
    id VARCHAR(255) PRIMARY KEY,
    remote_id INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    installed_version INTEGER,
    server_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_manifest_server FOREIGN KEY (server_id) REFERENCES server (id) ON DELETE CASCADE,
    CONSTRAINT uq_manifest_server_remote UNIQUE (server_id, remote_id)
);

-- Join table linking manifests to the quality checks they provide
CREATE TABLE manifest_quality_check (
    manifest_id VARCHAR(255) NOT NULL,
    quality_check_id INTEGER NOT NULL,
    PRIMARY KEY (manifest_id, quality_check_id),
    CONSTRAINT fk_manifest_check_manifest FOREIGN KEY (manifest_id) REFERENCES manifest (id) ON DELETE CASCADE,
    CONSTRAINT fk_manifest_check_check FOREIGN KEY (quality_check_id) REFERENCES quality_check (id) ON DELETE CASCADE
);
