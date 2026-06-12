-- Remove default value from epsilon_budget column by recreating the table (SQLite)
CREATE TABLE quality_check_new
(
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    name              VARCHAR(255),
    description       VARCHAR(1024),
    query             TEXT,
    warning_threshold INTEGER,
    error_threshold   INTEGER,
    epsilon_budget    REAL,
    check_type        VARCHAR(10) NOT NULL
);

INSERT INTO quality_check_new (id, name, description, query, warning_threshold, error_threshold, epsilon_budget,
                               check_type)
SELECT id,
       name,
       description,
       query,
       warning_threshold,
       error_threshold,
       epsilon_budget,
       check_type
FROM quality_check;

DROP TABLE quality_check;

ALTER TABLE quality_check_new
    RENAME TO quality_check;

-- Add OMOP CDM equivalents of the built-in FHIR/CQL quality checks
INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Missing Gender attribute',
        'How many persons do not have the gender_concept_id attribute',
        'SELECT person_id FROM person WHERE gender_concept_id IS NULL',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Impossible birth year (in the future or before 20th century)',
        'How many persons have birth years before 1900 or in the future',
        'SELECT person_id FROM person WHERE year_of_birth < 1900 OR year_of_birth > EXTRACT(YEAR FROM CURRENT_DATE)',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Gender-Incompatible Diagnosis',
        'How many persons have a diagnosis incompatible with their gender based on source value codes',
        'SELECT DISTINCT p.person_id FROM person p JOIN condition_occurrence c ON c.person_id = p.person_id WHERE (p.gender_concept_id = 8532 AND c.condition_source_value LIKE ''C61%'') OR (p.gender_concept_id = 8507 AND c.condition_source_value LIKE ''R80.2%'')',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Missing diagnosis or condition',
        'How many persons have no condition_occurrence record',
        'SELECT person_id FROM person p WHERE NOT EXISTS (SELECT 1 FROM condition_occurrence c WHERE c.person_id = p.person_id)',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Unsupported Gender Value',
        'How many persons have unsupported or undefined gender_concept_id values',
        'SELECT person_id FROM person WHERE gender_concept_id NOT IN (8507, 8532, 8521, 8570, 8551) OR gender_concept_id IS NULL',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: No recent visit in the last year',
        'How many persons have no visit_occurrence record in the last year or have no visits at all',
        'SELECT p.person_id FROM person p LEFT JOIN (SELECT person_id, MAX(visit_start_date) AS max_visit_date FROM visit_occurrence GROUP BY person_id) v ON v.person_id = p.person_id WHERE v.max_visit_date IS NULL OR v.max_visit_date < CURRENT_DATE - INTERVAL ''1 year''',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Conditions without onset date',
        'How many persons have condition_occurrence records without a condition_start_date',
        'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM condition_occurrence c WHERE c.person_id = p.person_id AND c.condition_start_date IS NULL)',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Persons without source identifier',
        'How many persons do not have a person_source_value identifier',
        'SELECT person_id FROM person WHERE person_source_value IS NULL OR person_source_value = ''''',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Persons without birth year',
        'How many persons do not have a year_of_birth',
        'SELECT person_id FROM person WHERE year_of_birth IS NULL',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Specimens without collection date',
        'How many persons have specimen records without a specimen_date',
        'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM specimen s WHERE s.person_id = p.person_id AND s.specimen_date IS NULL)',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Specimens without specimen concept',
        'How many persons have specimen records with missing or zero specimen_concept_id',
        'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM specimen s WHERE s.person_id = p.person_id AND (s.specimen_concept_id IS NULL OR s.specimen_concept_id = 0))',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Specimens without anatomic site concept',
        'How many persons have specimen records with missing or zero anatomic_site_concept_id',
        'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM specimen s WHERE s.person_id = p.person_id AND (s.anatomic_site_concept_id IS NULL OR s.anatomic_site_concept_id = 0))',
        'SQL', 1, 10);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold)
VALUES ('OMOP: Specimens without specimen type concept',
        'How many persons have specimen records with missing or zero specimen_type_concept_id',
        'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM specimen s WHERE s.person_id = p.person_id AND (s.specimen_type_concept_id IS NULL OR s.specimen_type_concept_id = 0))',
        'SQL', 1, 10);
