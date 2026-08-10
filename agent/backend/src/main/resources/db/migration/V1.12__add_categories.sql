-- Create category table
CREATE TABLE category
(
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name      VARCHAR(255) NOT NULL UNIQUE,
    color_hex VARCHAR(7)
);

-- Add category reference to quality_check
ALTER TABLE quality_check
    ADD COLUMN category_id INTEGER REFERENCES category (id) ON DELETE SET NULL;
