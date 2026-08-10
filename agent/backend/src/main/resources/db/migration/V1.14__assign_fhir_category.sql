-- Create the FHIR category if it does not already exist
INSERT OR IGNORE INTO category (name, color_hex)
VALUES ('FHIR', '#ff9200');

-- Assign the FHIR category to all remaining uncategorized quality checks that are not OMOP checks
UPDATE quality_check
SET category_id = (SELECT id FROM category WHERE name = 'FHIR')
WHERE category_id IS NULL
  AND LOWER(name) NOT LIKE '%omop%';
