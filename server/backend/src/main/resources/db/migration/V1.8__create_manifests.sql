CREATE TABLE manifest
(
    id           BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL DEFAULT '',
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    body         TEXT                     NOT NULL,
    signature    TEXT,
    key_id       VARCHAR(255)
);