-- ============================================
-- Create Category Table
-- ============================================

-- Create category table for grouping quality checks
CREATE TABLE category (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    color_hex TEXT
);

-- Add index on name for better query performance
CREATE INDEX idx_category_name ON category(name);

