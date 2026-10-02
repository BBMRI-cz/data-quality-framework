-- Create audit_log table
CREATE TABLE audit_log
(
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TIMESTAMP,
    actor     VARCHAR(255),
    actor_id  INTEGER REFERENCES user_account (id) ON DELETE SET NULL,
    action    VARCHAR(50),
    details   TEXT,
    module    VARCHAR(100),
    entity_id INTEGER
);
