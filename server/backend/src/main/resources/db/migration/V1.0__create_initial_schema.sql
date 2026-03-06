CREATE TABLE agent
(
    id      TEXT PRIMARY KEY,
    name    TEXT,
    status  TEXT,
    version TEXT DEFAULT 'Unknown'
);

CREATE INDEX idx_agent_status ON agent (status);

CREATE TABLE user_account
(
    id         BIGSERIAL PRIMARY KEY,
    username   TEXT NOT NULL UNIQUE,
    password   TEXT,
    agent_id   TEXT,
    subject_id TEXT UNIQUE,
    FOREIGN KEY (agent_id) REFERENCES agent (id)
);

CREATE TABLE user_roles
(
    user_id BIGINT NOT NULL,
    role    TEXT   NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles (user_id);

CREATE TABLE report
(
    id             TEXT PRIMARY KEY,
    timestamp      timestamptz NOT NULL DEFAULT NOW(),
    agent_id       TEXT        NOT NULL,
    total_patients INTEGER,
    total_samples  INTEGER,
    FOREIGN KEY (agent_id) REFERENCES agent (id)
);

CREATE INDEX idx_report_agent_id ON report (agent_id);
CREATE INDEX idx_report_timestamp ON report (timestamp);

CREATE TABLE quality_check
(
    hash              TEXT PRIMARY KEY,
    name              TEXT             NOT NULL,
    description       TEXT,
    registered_at     timestamptz      NOT NULL DEFAULT NOW(),
    warning_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    error_threshold   DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    category_id       BIGINT
);

CREATE INDEX idx_quality_check_name ON quality_check (name);
CREATE INDEX idx_quality_check_registered_at ON quality_check (registered_at);
CREATE INDEX idx_quality_check_category_id ON quality_check (category_id);

CREATE TABLE quality_check_result
(
    report_id          TEXT             NOT NULL,
    quality_check_hash TEXT             NOT NULL,
    result             DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (report_id, quality_check_hash),
    FOREIGN KEY (report_id) REFERENCES report (id) ON DELETE CASCADE,
    FOREIGN KEY (quality_check_hash) REFERENCES quality_check (hash) ON DELETE CASCADE
);

CREATE INDEX idx_result_quality_check_hash ON quality_check_result (quality_check_hash);

CREATE TABLE agent_interaction
(
    id        TEXT PRIMARY KEY,
    timestamp timestamptz NOT NULL DEFAULT NOW(),
    type      TEXT        NOT NULL,
    agent_id  TEXT        NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES agent (id) ON DELETE CASCADE
);

CREATE INDEX idx_agent_interaction_agent_id ON agent_interaction (agent_id);
CREATE INDEX idx_agent_interaction_timestamp ON agent_interaction (timestamp);
CREATE INDEX idx_agent_interaction_type ON agent_interaction (type);

CREATE TABLE setting
(
    setting_name  TEXT PRIMARY KEY,
    setting_value TEXT
);

CREATE TABLE category
(
    id        BIGSERIAL PRIMARY KEY,
    name      TEXT NOT NULL UNIQUE,
    color_hex TEXT
);

CREATE INDEX idx_category_name ON category (name);

ALTER TABLE quality_check
    ADD CONSTRAINT fk_quality_check_category
        FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL;

CREATE TABLE agent_group
(
    id   BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE INDEX idx_agent_group_name ON agent_group (name);

CREATE TABLE group_agent
(
    group_id BIGINT NOT NULL,
    agent_id TEXT   NOT NULL,
    PRIMARY KEY (group_id, agent_id),
    FOREIGN KEY (group_id) REFERENCES agent_group (id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agent (id) ON DELETE CASCADE
);

CREATE INDEX idx_group_agent_group_id ON group_agent (group_id);
CREATE INDEX idx_group_agent_agent_id ON group_agent (agent_id);

INSERT INTO user_account (username, password)
VALUES ('admin', '$argon2id$v=19$m=19456,t=2,p=1$SQGK8wXpQw5b+qjuq/Ih1A$WP87YsUIErq6O+7rMk65U0cH4OHBRdrnM3yIG50gwpE');

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN'
FROM user_account
WHERE username = 'admin';
INSERT INTO user_roles (user_id, role)
SELECT id, 'HUMAN_USER'
FROM user_account
WHERE username = 'admin';

INSERT INTO setting (setting_name, setting_value)
VALUES ('oidcAuthority', NULL),
       ('oidcClientId', NULL),
       ('oidcRedirectUri', NULL),
       ('oidcPostLogoutRedirectUri', NULL),
       ('oidcScopes', NULL),
       ('oidcAuthorityName', NULL),
       ('oidcAuthorityLogo', NULL),
       ('oidcSwaggerRedirectUrl', 'http://localhost:8082/api/swagger-ui/oauth2-redirect.html');
