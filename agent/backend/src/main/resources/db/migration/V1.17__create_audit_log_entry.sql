-- Create audit_log_entry table
CREATE TABLE audit_log_entry
(
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TIMESTAMP,
    actor     VARCHAR(255),
    action    VARCHAR(50),
    details   TEXT,
    module    VARCHAR(100),
    entity_id INTEGER
);
