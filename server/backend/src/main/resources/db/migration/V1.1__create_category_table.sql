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

-- ============================================
-- Add Category Relationship to Quality Check
-- ============================================

-- Add category_id column to quality_check table
-- ON DELETE SET NULL ensures that when a category is deleted,
-- the category_id in quality_check is set to NULL
ALTER TABLE quality_check ADD COLUMN category_id INTEGER REFERENCES category(id) ON DELETE SET NULL;

-- Add index on category_id for better query performance
CREATE INDEX idx_quality_check_category_id ON quality_check(category_id);

