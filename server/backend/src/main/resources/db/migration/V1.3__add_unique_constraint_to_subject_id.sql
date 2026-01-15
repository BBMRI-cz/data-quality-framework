-- ============================================
-- Migration V1.1 - Add OIDC Support
-- ============================================
-- This migration adds OIDC support by:
-- 1. Making password nullable (OIDC users don't need passwords)
-- 2. Adding subject_id column with UNIQUE constraint for OIDC subject identifiers

-- SQLite doesn't support ALTER TABLE modifications for changing constraints,
-- so we need to recreate the table with the new schema.

-- First, save user_roles data
CREATE TEMPORARY TABLE user_roles_backup (
    user_id INTEGER NOT NULL,
    role TEXT NOT NULL
);

INSERT INTO user_roles_backup SELECT user_id, role FROM user_roles;

-- Drop user_roles (has FK to user_account)
DROP TABLE user_roles;

-- Create new user_account table with updated schema
CREATE TABLE user_account_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT,  -- Made nullable for OIDC users
    agent_id TEXT,
    subject_id TEXT UNIQUE,  -- Added for OIDC subject identifier
    FOREIGN KEY (agent_id) REFERENCES agent(id)
);

-- Copy data from old table to new table
INSERT INTO user_account_new (id, username, password, agent_id, subject_id)
SELECT id, username, password, agent_id, NULL
FROM user_account;

DROP TABLE user_account;
ALTER TABLE user_account_new RENAME TO user_account;

CREATE TABLE user_roles (
    user_id INTEGER NOT NULL,
    role TEXT NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES user_account(id) ON DELETE CASCADE
);

-- Create index for performance on user_id lookups
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

INSERT INTO user_roles SELECT user_id, role FROM user_roles_backup;
DROP TABLE user_roles_backup;

