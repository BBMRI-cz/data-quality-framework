-- Add check_type column to quality_check table
ALTER TABLE quality_check ADD COLUMN check_type VARCHAR(10) DEFAULT 'CQL' NOT NULL;

-- Insert built-in quality checks with NULL query (non-CQL checks)
INSERT INTO quality_check (id, name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES (1000, 'Duplicate identifiers', 'Duplicate patients', NULL, 'JAVA', 10, 20, 0.2);

INSERT INTO quality_check (id, name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES (1001, 'Invalid ICD-10 Codes', 'How many conditions have invalid ICD-10 codes', NULL, 'JAVA', 10, 30, 0.2);

INSERT INTO quality_check (id, name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES (1002, 'Survival rate, stratified per gender value', 'What is the survival rate for different gender values', NULL, 'JAVA', 70, 90, 0.2);

INSERT INTO quality_check (id, name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES (1003, 'Patients last updated more than three months ago', 'Patients last updated more than three months ago', NULL, 'JAVA', 10, 30, 0.2);


