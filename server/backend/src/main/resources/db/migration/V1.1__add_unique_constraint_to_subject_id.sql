-- ============================================
-- Migration V1.1 - Add Unique Constraint to subject_id
-- ============================================
-- This migration adds a unique constraint to the subject_id column in user_account table
-- to ensure that each subject_id (from OIDC or other identity providers) is unique.

-- SQLite doesn't support ALTER TABLE ADD CONSTRAINT for UNIQUE constraints,
-- so we need to recreate the table with the constraint.
CREATE TABLE user_account_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT,
    agent_id TEXT,
    subject_id TEXT UNIQUE,  -- Added UNIQUE constraint
    FOREIGN KEY (agent_id) REFERENCES agent(id)
);

-- Copy data from old table to new table
-- If there are duplicate subject_ids, only the first one will be kept
INSERT INTO user_account_new (id, username, password, agent_id, subject_id)
SELECT id, username, password, agent_id, subject_id
FROM user_account
WHERE subject_id IS NULL
   OR subject_id NOT IN (
    SELECT subject_id
    FROM user_account
    WHERE subject_id IS NOT NULL
    GROUP BY subject_id
    HAVING COUNT(*) > 1
)
UNION
SELECT MIN(id), MIN(username), MIN(password), MIN(agent_id), subject_id
FROM user_account
WHERE subject_id IS NOT NULL
GROUP BY subject_id;

DROP TABLE user_account;

ALTER TABLE user_account_new RENAME TO user_account;


