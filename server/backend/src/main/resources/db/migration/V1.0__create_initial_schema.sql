

-- ============================================
-- Initial Schema - Consolidated Migration
-- ============================================

-- Create agent table first (referenced by user_account)
CREATE TABLE agent (
                       id TEXT PRIMARY KEY,
                       name TEXT,
                       status TEXT,
                       version TEXT DEFAULT 'Unknown'
);

-- Add index on status for better query performance
CREATE INDEX idx_agent_status ON agent(status);

-- Create user_account table with foreign key to agent
CREATE TABLE user_account (
                              id INTEGER PRIMARY KEY AUTOINCREMENT,
                              username TEXT NOT NULL UNIQUE,
                              password TEXT NOT NULL,
                              agent_id TEXT,
                              FOREIGN KEY (agent_id) REFERENCES agent(id)
);

-- Create user_roles table to store user role assignments
CREATE TABLE user_roles (
                            user_id INTEGER NOT NULL,
                            role TEXT NOT NULL,
                            PRIMARY KEY (user_id, role),
                            FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE
);

-- Create index for performance on user_id lookups
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Create report table
CREATE TABLE report (
                        id TEXT PRIMARY KEY,
                        timestamp TEXT NOT NULL,
                        agent_id TEXT NOT NULL,
                        FOREIGN KEY (agent_id) REFERENCES agent(id)
);

-- Add indexes on report table
CREATE INDEX idx_report_agent_id ON report(agent_id);
CREATE INDEX idx_report_timestamp ON report(timestamp);

-- Create quality_check table
CREATE TABLE quality_check (
                               hash TEXT PRIMARY KEY,
                               name TEXT NOT NULL,
                               description TEXT,
                               registered_at TEXT NOT NULL,
                               warning_threshold REAL NOT NULL DEFAULT 0.0,
                               error_threshold REAL NOT NULL DEFAULT 0.0
);

-- Create quality_check_result table (join table with result data)
CREATE TABLE quality_check_result (
                                      report_id TEXT NOT NULL,
                                      quality_check_hash TEXT NOT NULL,
                                      result REAL NOT NULL,
                                      PRIMARY KEY (report_id, quality_check_hash),
                                      FOREIGN KEY (report_id) REFERENCES report(id) ON DELETE CASCADE,
                                      FOREIGN KEY (quality_check_hash) REFERENCES quality_check(hash) ON DELETE CASCADE
);

-- Add indexes for quality_check tables
CREATE INDEX idx_quality_check_name ON quality_check(name);
CREATE INDEX idx_quality_check_registered_at ON quality_check(registered_at);
CREATE INDEX idx_result_quality_check_hash ON quality_check_result(quality_check_hash);

-- Create agent_interaction table
CREATE TABLE agent_interaction (
                                   id TEXT PRIMARY KEY,
                                   timestamp TEXT NOT NULL,
                                   type TEXT NOT NULL,
                                   agent_id TEXT NOT NULL,
                                   FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
);

-- Add indexes on agent_interaction table
CREATE INDEX idx_agent_interaction_agent_id ON agent_interaction(agent_id);
CREATE INDEX idx_agent_interaction_timestamp ON agent_interaction(timestamp);
CREATE INDEX idx_agent_interaction_type ON agent_interaction(type);

-- Create settings table
CREATE TABLE setting (
                         setting_name TEXT PRIMARY KEY,
                         setting_value TEXT NOT NULL
);

-- ============================================
-- Initial Data
-- ============================================

-- Insert default admin user
INSERT INTO user_account (username, password)
VALUES ('admin', '$argon2id$v=19$m=19456,t=2,p=1$SQGK8wXpQw5b+qjuq/Ih1A$WP87YsUIErq6O+7rMk65U0cH4OHBRdrnM3yIG50gwpE');

-- Assign both ADMIN and HUMAN_USER roles to the admin user
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM user_account WHERE username = 'admin';
INSERT INTO user_roles (user_id, role)
SELECT id, 'HUMAN_USER' FROM user_account WHERE username = 'admin';
