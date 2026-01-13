-- ============================================
-- Create Group Table and Agent-Group Relationship
-- ============================================

-- Create agent_group table for organizing agents
CREATE TABLE agent_group (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

-- Add index on name for better query performance
CREATE INDEX idx_agent_group_name ON agent_group(name);

-- Create join table for many-to-many relationship between agents and groups
CREATE TABLE group_agent (
    group_id INTEGER NOT NULL,
    agent_id TEXT NOT NULL,
    PRIMARY KEY (group_id, agent_id),
    FOREIGN KEY (group_id) REFERENCES agent_group(id) ON DELETE CASCADE,
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
);

-- Add indexes for better query performance
CREATE INDEX idx_group_agent_group_id ON group_agent(group_id);
CREATE INDEX idx_group_agent_agent_id ON group_agent(agent_id);

