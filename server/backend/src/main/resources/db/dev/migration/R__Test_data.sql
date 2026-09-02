INSERT INTO agent (id, name, status) VALUES
    ('agent-001', 'Hospital Alpha Data Center', 'ACTIVE'),
    ('agent-002', 'Research Institute Beta',   'ACTIVE'),
    ('agent-003', 'Medical Center Gamma',      'INACTIVE'),
    ('agent-004', 'Biobank Delta Repository',  'ACTIVE'),
    ('agent-005', 'Clinical Data Hub Epsilon', 'ACTIVE');

-- Groups ----------------------------------------------------------------
INSERT INTO agent_group (id, name) VALUES
    (1, 'European Medical Centers'),
    (2, 'Research Facilities'),
    (3, 'Biobanks'),
    (4, 'Clinical Data Hubs'),
    (5, 'Inactive Facilities');

WITH group_members(group_id, agent_id) AS (VALUES
    (1, 'agent-001'), (1, 'agent-002'), (2, 'agent-002'),
    (1, 'agent-003'), (5, 'agent-003'),
    (3, 'agent-004'),
    (1, 'agent-005'), (4, 'agent-005'))
INSERT INTO group_agent (group_id, agent_id)
SELECT * FROM group_members;

-- Categories ------------------------------------------------------------
INSERT INTO category (name, color_hex) VALUES
    ('Data Completeness', '#2196F3'),
    ('Data Validity',     '#FF9800'),
    ('Data Consistency',  '#4CAF50');

-- Quality checks --------------------------------------------------------
INSERT INTO quality_check (name, description, warning_threshold, error_threshold, category_id)
SELECT name, description, warn_threshold, err_threshold, category_id
FROM (VALUES
    ('Unsupported Gender Values', 'Percentage of patients with non-supported gender attribute values (not Male/Female/Other/Unknown)', 5.0, 15.0, 2),
    ('Missing Birth Date',        'Percentage of patients with missing or null birth date values', 3.0, 10.0, 1),
    ('Invalid Date Values',       'Percentage of records with logically invalid dates (e.g., future birth dates, death before birth)', 2.0, 8.0, 2),
    ('Duplicate Patient Records', 'Percentage of patients that appear to be duplicates based on matching identifiers', 1.0, 5.0, 3),
    ('Invalid Data Formats',      'Percentage of records with data that does not follow expected formats (e.g., malformed IDs, invalid postal codes)', 4.0, 12.0, NULL),
    ('Broken Reference Integrity','Percentage of records with references to non-existent related entities', 2.0, 8.0, 1),
    ('Statistical Outlier Values','Percentage of numerical values that are statistical outliers (e.g., age > 150, negative measurements)', 10.0, 25.0, NULL),
    ('Invalid Medical Codes',     'Percentage of records with invalid or non-standard medical codes (ICD-10, SNOMED CT)', 3.0, 10.0, 2)
) AS c(name, description, warn_threshold, err_threshold, category_id);

-- Quality check versions (initial v1). Some queries are empty to model legacy checks.
WITH ver(name, query, hash) AS (VALUES
    ('Unsupported Gender Values', 'SELECT COUNT(*) AS total, SUM(CASE WHEN gender NOT IN (''Male'', ''Female'', ''Other'', ''Unknown'') THEN 1 ELSE 0 END) AS invalid FROM patients;', 'unsupported-gender-check'),
    ('Missing Birth Date',        'SELECT COUNT(*) AS total, SUM(CASE WHEN birthdate IS NULL THEN 1 ELSE 0 END) AS invalid FROM patients;', 'missing-birthdate-check'),
    ('Invalid Date Values',       'SELECT COUNT(*) AS total, SUM(CASE WHEN birthdate > CURRENT_DATE OR (death_date IS NOT NULL AND death_date < birthdate) THEN 1 ELSE 0 END) AS invalid FROM patients;', 'invalid-date-check'),
    ('Duplicate Patient Records', 'SELECT COUNT(*) AS total, COUNT(*) - COUNT(DISTINCT mrn) AS invalid FROM patients;', 'duplicate-patient-check'),
    ('Invalid Data Formats',      '', 'invalid-format-check'),
    ('Broken Reference Integrity','', 'broken-reference-check'),
    ('Statistical Outlier Values','', 'outlier-value-check'),
    ('Invalid Medical Codes',     'SELECT COUNT(*) AS total, SUM(CASE WHEN icd10 NOT ~ ''^[A-Z][0-9]{2}(\\.[0-9]+)?$'' THEN 1 ELSE 0 END) AS invalid FROM diagnoses;', 'invalid-coding-check')
)
INSERT INTO quality_check_version (quality_check_id, version, query, hash)
SELECT q.id, 1, ver.query, ver.hash
FROM ver
JOIN quality_check q ON q.name = ver.name;

-- Keywords --------------------------------------------------------------
WITH kw(check_name, keyword) AS (VALUES
    ('Unsupported Gender Values', 'gender'),
    ('Unsupported Gender Values', 'sex'),
    ('Unsupported Gender Values', 'female'),
    ('Unsupported Gender Values', 'male'),
    ('Unsupported Gender Values', 'gender identity'),
    ('Missing Birth Date',        'birth date'),
    ('Missing Birth Date',        'birthdate'),
    ('Missing Birth Date',        'age'),
    ('Missing Birth Date',        'completeness'),
    ('Missing Birth Date',        'missing data'),
    ('Invalid Date Values',       'date'),
    ('Invalid Date Values',       'invalid date'),
    ('Invalid Date Values',       'future date'),
    ('Invalid Date Values',       'past date'),
    ('Duplicate Patient Records', 'duplicate'),
    ('Duplicate Patient Records', 'patient'),
    ('Duplicate Patient Records', 'merge'),
    ('Duplicate Patient Records', 'de duplication'),
    ('Invalid Data Formats',      'format'),
    ('Invalid Data Formats',      'postal code'),
    ('Invalid Data Formats',      'email'),
    ('Invalid Data Formats',      'pattern'),
    ('Broken Reference Integrity','reference'),
    ('Broken Reference Integrity','foreign key'),
    ('Broken Reference Integrity','orphan'),
    ('Broken Reference Integrity','integrity'),
    ('Statistical Outlier Values','outlier'),
    ('Statistical Outlier Values','statistical'),
    ('Statistical Outlier Values','abnormal value'),
    ('Statistical Outlier Values','extreme value'),
    ('Invalid Medical Codes',     'coding'),
    ('Invalid Medical Codes',     'ICD-10'),
    ('Invalid Medical Codes',     'SNOMED CT'),
    ('Invalid Medical Codes',     'diagnosis'),
    ('Invalid Medical Codes',     'C50')
)
INSERT INTO quality_check_keyword (quality_check_id, keyword)
SELECT q.id, kw.keyword
FROM kw
JOIN quality_check q ON q.name = kw.check_name;

-- Reports (regular, past) ------------------------------------------------
-- Some reports have NULL totals to exercise backwards-compatible display.
INSERT INTO report (id, timestamp, agent_id, total_patients, total_samples)
SELECT 'report-' || lpad(idx::text, 3, '0'),
       TIMESTAMP '2024-10-01 09:00:00+00' + (idx - 1) * INTERVAL '1 day',
       'agent-' || lpad(((idx - 1) % 5 + 1)::text, 3, '0'),
       CASE WHEN idx % 3 = 0 THEN NULL ELSE 1000 + idx * 50 END,
       CASE WHEN idx % 3 = 0 THEN NULL ELSE 3000 + idx * 100 END
FROM generate_series(1, 8) idx;

-- Recent reports ---------------------------------------------------------
INSERT INTO report (id, timestamp, agent_id, total_patients, total_samples)
VALUES
    ('report-current-1', NOW() - INTERVAL '2 hours',    'agent-001', 1275, 3850),
    ('report-current-2', NOW() - INTERVAL '1 hour',     'agent-002', 895,  2475),
    ('report-current-3', NOW() - INTERVAL '30 minutes', 'agent-003', NULL, NULL);

-- Quality check results (one per report x check, linking the v1 version) --
WITH checks(idx, name) AS (VALUES
    (1, 'Unsupported Gender Values'),
    (2, 'Missing Birth Date'),
    (3, 'Invalid Date Values'),
    (4, 'Duplicate Patient Records'),
    (5, 'Invalid Data Formats'),
    (6, 'Broken Reference Integrity'),
    (7, 'Statistical Outlier Values'),
    (8, 'Invalid Medical Codes'))
INSERT INTO quality_check_result (report_id, quality_check_id, version_id, result)
SELECT 'report-' || lpad(r::text, 3, '0'),
       q.id,
       v.id,
       round(((r * 7 + c.idx * 3) % 20)::numeric / 100.0, 2)
FROM generate_series(1, 8) r
CROSS JOIN checks c
JOIN quality_check q ON q.name = c.name
JOIN quality_check_version v ON v.quality_check_id = q.id AND v.version = 1;

-- Results for the recent reports -----------------------------------------
WITH recent(check_name) AS (VALUES
    ('Unsupported Gender Values'), ('Missing Birth Date'), ('Invalid Date Values'))
INSERT INTO quality_check_result (report_id, quality_check_id, version_id, result)
SELECT 'report-current-1', q.id, v.id, 0.02
FROM recent
JOIN quality_check q ON q.name = recent.check_name
JOIN quality_check_version v ON v.quality_check_id = q.id AND v.version = 1;

INSERT INTO quality_check_result (report_id, quality_check_id, version_id, result)
SELECT 'report-current-3', q.id, v.id, NULL
FROM quality_check q
JOIN quality_check_version v ON v.quality_check_id = q.id AND v.version = 1
WHERE q.name IN ('Missing Birth Date', 'Invalid Medical Codes');

-- Agent interactions -----------------------------------------------------
-- Registrations
INSERT INTO agent_interaction (id, timestamp, type, agent_id)
SELECT 'interaction-reg-' || lpad(idx::text, 3, '0'),
       TIMESTAMP '2024-01-15 08:00:00+00' + (idx - 1) * INTERVAL '10 days',
       'REGISTRATION',
       'agent-' || lpad(idx::text, 3, '0')
FROM generate_series(1, 5) idx;

-- Regular pings (4 per active agent)
INSERT INTO agent_interaction (id, timestamp, type, agent_id)
SELECT 'interaction-ping-' || lpad(a::text, 3, '0') || '-' || p::text,
       TIMESTAMP '2025-10-23 08:00:00+00' - p * INTERVAL '1 day',
       'PING',
       'agent-' || lpad(a::text, 3, '0')
FROM generate_series(1, 5) a
CROSS JOIN generate_series(0, 3) p;

-- Report interactions (matching the generated reports)
INSERT INTO agent_interaction (id, timestamp, type, agent_id)
SELECT 'interaction-report-' || lpad(idx::text, 3, '0'),
       TIMESTAMP '2024-10-01 09:00:00+00' + (idx - 1) * INTERVAL '1 day',
       'REPORT',
       'agent-' || lpad(((idx - 1) % 5 + 1)::text, 3, '0')
FROM generate_series(1, 8) idx;
