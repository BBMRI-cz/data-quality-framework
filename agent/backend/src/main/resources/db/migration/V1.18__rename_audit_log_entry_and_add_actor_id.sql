ALTER TABLE audit_log_entry RENAME TO audit_log;

-- Link audit log entries to the user account that performed the action, where known
ALTER TABLE audit_log
    ADD COLUMN actor_id INTEGER REFERENCES user_account (id) ON DELETE SET NULL;
