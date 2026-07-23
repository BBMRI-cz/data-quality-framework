-- Create the OMOP category if it does not already exist
INSERT OR IGNORE INTO category (name, color_hex)
VALUES ('OMOP', '#0d6efd');

-- Assign the OMOP category to all quality checks whose name contains "omop" (case-insensitive)
UPDATE quality_check
SET category_id = (SELECT id FROM category WHERE name = 'OMOP')
WHERE LOWER(name) LIKE '%omop%';
