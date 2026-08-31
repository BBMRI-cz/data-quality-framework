INSERT INTO agent (id, name, status) VALUES
    ('agent-001', 'Hospital Alpha Data Center', 'ACTIVE'),
    ('agent-002', 'Research Institute Beta', 'ACTIVE'),
    ('agent-003', 'Medical Center Gamma', 'INACTIVE'),
    ('agent-004', 'Biobank Delta Repository', 'ACTIVE'),
    ('agent-005', 'Clinical Data Hub Epsilon', 'ACTIVE');

-- Insert groups for organizing agents
INSERT INTO agent_group (id, name) VALUES
    (1, 'European Medical Centers'),
    (2, 'Research Facilities'),
    (3, 'Biobanks'),
    (4, 'Clinical Data Hubs'),
    (5, 'Inactive Facilities');

-- Link agents to groups (many-to-many relationship)
INSERT INTO group_agent (group_id, agent_id) VALUES
    -- Hospital Alpha is a European Medical Center
    (1, 'agent-001'),
    -- Research Institute Beta is both a European Medical Center and Research Facility
    (1, 'agent-002'),
    (2, 'agent-002'),
    -- Medical Center Gamma is inactive
    (1, 'agent-003'),
    (5, 'agent-003'),
    -- Biobank Delta is in Biobanks group
    (3, 'agent-004'),
    -- Clinical Data Hub Epsilon is in multiple groups
    (1, 'agent-005'),
    (4, 'agent-005');

-- Insert categories for organizing quality checks
INSERT INTO category (name, color_hex) VALUES
    ('Data Completeness', '#2196F3'),
    ('Data Validity', '#FF9800'),
    ('Data Consistency', '#4CAF50');

-- Insert dummy quality checks
-- NOTE: Result values represent the FRACTION of INVALID records (0.0 = perfect, 1.0 = all invalid)
-- warning_threshold: if result% > warning_threshold, trigger warning
-- error_threshold: if result% > error_threshold, trigger error
-- The id column is BIGSERIAL and auto-generated. The hash is the unique business key.
INSERT INTO quality_check (hash, name, description, registered_at, warning_threshold, error_threshold, category_id) VALUES
    ('unsupported-gender-check', 'Unsupported Gender Values', 'Percentage of patients with non-supported gender attribute values (not Male/Female/Other/Unknown)', '2024-01-15T10:00:00+00:00', 5.0, 15.0, 2),
    ('missing-birthdate-check', 'Missing Birth Date', 'Percentage of patients with missing or null birth date values', '2024-01-15T10:15:00+00:00', 3.0, 10.0, 1),
    ('invalid-date-check', 'Invalid Date Values', 'Percentage of records with logically invalid dates (e.g., future birth dates, death before birth)', '2024-01-15T10:30:00+00:00', 2.0, 8.0, 2),
    ('duplicate-patient-check', 'Duplicate Patient Records', 'Percentage of patients that appear to be duplicates based on matching identifiers', '2024-01-15T10:45:00+00:00', 1.0, 5.0, 3),
    ('invalid-format-check', 'Invalid Data Formats', 'Percentage of records with data that does not follow expected formats (e.g., malformed IDs, invalid postal codes)', '2024-01-15T11:00:00+00:00', 4.0, 12.0, NULL),
    ('broken-reference-check', 'Broken Reference Integrity', 'Percentage of records with references to non-existent related entities', '2024-01-15T11:15:00+00:00', 2.0, 8.0, 1),
    ('outlier-value-check', 'Statistical Outlier Values', 'Percentage of numerical values that are statistical outliers (e.g., age > 150, negative measurements)', '2024-01-15T11:30:00+00:00', 10.0, 25.0, NULL),
    ('invalid-coding-check', 'Invalid Medical Codes', 'Percentage of records with invalid or non-standard medical codes (ICD-10, SNOMED CT)', '2024-01-15T11:45:00+00:00', 3.0, 10.0, 2);

-- Insert quality check versions
-- Each quality check gets an initial (v1) version. Some versions carry a real query body, while
-- others (whose original query is no longer available) have an empty query body to model legacy checks.
-- The version hash reuses the quality check's business-key hash so the two stay aligned.
INSERT INTO quality_check_version (quality_check_id, version, query, hash) VALUES
    ((SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 1,
     'SELECT COUNT(*) AS total, SUM(CASE WHEN gender NOT IN (''Male'', ''Female'', ''Other'', ''Unknown'') THEN 1 ELSE 0 END) AS invalid FROM patients;',
     'unsupported-gender-check'),
    ((SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 1,
     'SELECT COUNT(*) AS total, SUM(CASE WHEN birthdate IS NULL THEN 1 ELSE 0 END) AS invalid FROM patients;',
     'missing-birthdate-check'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 1,
     'SELECT COUNT(*) AS total, SUM(CASE WHEN birthdate > CURRENT_DATE OR (death_date IS NOT NULL AND death_date < birthdate) THEN 1 ELSE 0 END) AS invalid FROM patients;',
     'invalid-date-check'),
    ((SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 1,
     'SELECT COUNT(*) AS total, COUNT(*) - COUNT(DISTINCT mrn) AS invalid FROM patients;',
     'duplicate-patient-check'),
    -- Legacy checks without a stored query body
    ((SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 1, '', 'invalid-format-check'),
    ((SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 1, '', 'broken-reference-check'),
    ((SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 1, '', 'outlier-value-check'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 1,
     'SELECT COUNT(*) AS total, SUM(CASE WHEN icd10 NOT ~ ''^[A-Z][0-9]{2}(\\.[0-9]+)?$'' THEN 1 ELSE 0 END) AS invalid FROM diagnoses;',
     'invalid-coding-check');

-- Insert keywords for NLP-based search and filtering
-- Keywords help match user queries to relevant quality checks
-- Format: quality_check_id (resolved from hash), keyword (max 250 chars)
INSERT INTO quality_check_keyword (quality_check_id, keyword) VALUES
    -- Gender-related checks - for queries about gender, sex, female, male
    ((SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 'gender'),
    ((SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 'sex'),
    ((SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 'female'),
    ((SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 'male'),
    ((SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 'gender identity'),
    ((SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 'patient demographics'),
    ((SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 'C12.9'),

    -- Birth date / age-related checks
    ((SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 'birth date'),
    ((SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 'birthdate'),
    ((SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 'age'),
    ((SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 'date of birth'),
    ((SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 'missing data'),
    ((SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 'completeness'),

    -- Date-related checks
    ((SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 'date'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 'time'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 'future date'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 'past date'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 'invalid date'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 'logically invalid'),

    -- Patient duplicate checks
    ((SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 'duplicate'),
    ((SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 'patient'),
    ((SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 'same patient'),
    ((SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 'merge'),
    ((SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 'de duplication'),

    -- Format validation checks
    ((SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 'format'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 'postal code'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 'zip code'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 'phone'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 'email'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 'ID format'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 'pattern'),

    -- Reference integrity checks
    ((SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 'reference'),
    ((SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 'foreign key'),
    ((SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 'link'),
    ((SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 'relationship'),
    ((SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 'orphan'),
    ((SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 'integrity'),

    -- Outlier/value checks
    ((SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 'outlier'),
    ((SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 'statistical'),
    ((SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 'value'),
    ((SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 'age > 150'),
    ((SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 'negative value'),
    ((SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 'abnormal value'),
    ((SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 'extreme value'),

    -- Medical coding checks
    ((SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 'coding'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 'ICD-10'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 'SNOMED'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 'SNOMED CT'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 'medical code'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 'diagnosis'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 'C50'),
    ((SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 'condition');

-- Insert dummy reports for the past 30 days
-- Some reports include total_patients and total_samples, others are NULL for backwards compatibility testing
INSERT INTO report (id, timestamp, agent_id, total_patients, total_samples) VALUES
    ('report-001', '2024-10-01T09:00:00+00:00', 'agent-001', 1250, 3800),
    ('report-002', '2024-10-01T14:30:00+00:00', 'agent-002', 890, 2450),
    ('report-003', '2024-10-02T08:15:00+00:00', 'agent-001', NULL, NULL),  -- backwards compatibility test
    ('report-004', '2024-10-02T16:45:00+00:00', 'agent-004', 2100, 6500),
    ('report-005', '2024-10-03T10:20:00+00:00', 'agent-002', 920, 2600),
    ('report-006', '2024-10-03T13:10:00+00:00', 'agent-005', NULL, NULL),  -- backwards compatibility test
    ('report-007', '2024-10-04T11:30:00+00:00', 'agent-001', 1300, 3950),
    ('report-008', '2024-10-04T15:20:00+00:00', 'agent-004', 2150, 6700),
    ('report-009', '2024-10-05T09:45:00+00:00', 'agent-002', NULL, NULL),  -- backwards compatibility test
    ('report-010', '2024-10-05T14:15:00+00:00', 'agent-005', 1680, 5200),
    ('report-011', '2024-10-06T10:00:00+00:00', 'agent-001', 1280, 3900),
    ('report-012', '2024-10-06T16:30:00+00:00', 'agent-004', NULL, NULL),  -- backwards compatibility test
    ('report-013', '2024-10-07T08:45:00+00:00', 'agent-002', 905, 2520),
    ('report-014', '2024-10-07T12:20:00+00:00', 'agent-005', 1700, 5300),
    ('report-015', '2024-10-08T11:15:00+00:00', 'agent-001', 1310, 4000),
    ('report-016', '2024-10-08T15:40:00+00:00', 'agent-004', 2200, 6850),
    ('report-017', '2024-10-09T09:30:00+00:00', 'agent-002', NULL, NULL),  -- backwards compatibility test
    ('report-018', '2024-10-09T14:50:00+00:00', 'agent-005', 1720, 5400),
    ('report-019', '2024-10-10T10:45:00+00:00', 'agent-001', 1290, 3920),
    ('report-020', '2024-10-10T16:10:00+00:00', 'agent-004', 2180, 6750);

-- Insert quality check results for the reports
-- Report 1 results (good quality data - low error rates)
INSERT INTO quality_check_result (report_id, quality_check_id, result) VALUES
    ('report-001', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.02),
    ('report-001', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.01),
    ('report-001', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 0.01),
    ('report-001', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 0.0),
    ('report-001', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 0.03),
    ('report-001', (SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 0.01),
    ('report-001', (SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 0.08),
    ('report-001', (SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 0.02);

-- Report 2 results (mixed quality - some warnings)
INSERT INTO quality_check_result (report_id, quality_check_id, result) VALUES
    ('report-002', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.07),
    ('report-002', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.05),
    ('report-002', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 0.03),
    ('report-002', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 0.02),
    ('report-002', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 0.09),
    ('report-002', (SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 0.04),
    ('report-002', (SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 0.15),
    ('report-002', (SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 0.06);

-- Report 3 results (poor quality data - many errors above thresholds)
INSERT INTO quality_check_result (report_id, quality_check_id, result) VALUES
    ('report-003', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.18),
    ('report-003', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.12),
    ('report-003', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 0.09),
    ('report-003', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 0.06),
    ('report-003', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 0.14),
    ('report-003', (SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 0.11),
    ('report-003', (SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 0.28),
    ('report-003', (SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 0.13);

-- Report 4 results (excellent quality - very low error rates)
INSERT INTO quality_check_result (report_id, quality_check_id, result) VALUES
    ('report-004', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.0),
    ('report-004', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.01),
    ('report-004', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 0.0),
    ('report-004', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 0.0),
    ('report-004', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 0.01),
    ('report-004', (SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 0.0),
    ('report-004', (SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 0.05),
    ('report-004', (SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 0.01);

-- Report 5 results (average quality)
INSERT INTO quality_check_result (report_id, quality_check_id, result) VALUES
    ('report-005', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.06),
    ('report-005', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.04),
    ('report-005', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 0.03),
    ('report-005', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 0.02),
    ('report-005', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 0.07),
    ('report-005', (SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 0.03),
    ('report-005', (SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 0.12),
    ('report-005', (SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 0.05);

-- Add more results for remaining reports with varying quality scores
INSERT INTO quality_check_result (report_id, quality_check_id, result) VALUES
    -- Report 6 (good quality)
    ('report-006', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.02),
    ('report-006', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.02),
    ('report-006', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 0.01),
    ('report-006', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 0.01),

    -- Report 7 (poor quality - warnings)
    ('report-007', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.11),
    ('report-007', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.08),
    ('report-007', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 0.10),
    ('report-007', (SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 0.18),

    -- Report 8 (good quality)
    ('report-008', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.03),
    ('report-008', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.02),
    ('report-008', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 0.02),
    ('report-008', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 0.01),
    ('report-008', (SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 0.02),

    -- Report 9 (very poor quality - many errors)
    ('report-009', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.19),
    ('report-009', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.15),
    ('report-009', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 0.16),
    ('report-009', (SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 0.14),

    -- Report 10 (excellent quality)
    ('report-010', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.01),
    ('report-010', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.01),
    ('report-010', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 0.01),
    ('report-010', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 0.0),
    ('report-010', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 0.02),
    ('report-010', (SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 0.01),
    ('report-010', (SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), 0.06),
    ('report-010', (SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 0.02);

-- Add some recent reports with current timestamps for immediate testing
INSERT INTO report (id, timestamp, agent_id, total_patients, total_samples) VALUES
    ('report-current-1', NOW() - INTERVAL '2 hours', 'agent-001', 1275, 3850),
    ('report-current-2', NOW() - INTERVAL '1 hour', 'agent-002', 895, 2475),
    ('report-current-3', NOW() - INTERVAL '30 minutes', 'agent-004', 2125, 6600),
    ('report-null-001', NOW() - INTERVAL '15 minutes', 'agent-003', NULL, NULL);

-- Add results for current reports
INSERT INTO quality_check_result (report_id, quality_check_id, result) VALUES
    ('report-current-1', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.04),
    ('report-current-1', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.02),
    ('report-current-1', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), 0.01),
    ('report-current-2', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.09),
    ('report-current-2', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), 0.11),
    ('report-current-2', (SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), 0.08),
    ('report-current-3', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), 0.01),
    ('report-current-3', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), 0.02),
    ('report-current-3', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), 0.02),
    ('report-current-3', (SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), 0.01),
    ('report-null-001', (SELECT id FROM quality_check WHERE hash = 'unsupported-gender-check'), NULL),
    ('report-null-001', (SELECT id FROM quality_check WHERE hash = 'missing-birthdate-check'), NULL),
    ('report-null-001', (SELECT id FROM quality_check WHERE hash = 'invalid-date-check'), NULL),
    ('report-null-001', (SELECT id FROM quality_check WHERE hash = 'duplicate-patient-check'), NULL),
    ('report-null-001', (SELECT id FROM quality_check WHERE hash = 'invalid-format-check'), NULL),
    ('report-null-001', (SELECT id FROM quality_check WHERE hash = 'broken-reference-check'), NULL),
    ('report-null-001', (SELECT id FROM quality_check WHERE hash = 'outlier-value-check'), NULL),
    ('report-null-001', (SELECT id FROM quality_check WHERE hash = 'invalid-coding-check'), NULL);

-- Insert agent interactions
-- Registration interactions (when agents first joined)
INSERT INTO agent_interaction (id, timestamp, type, agent_id) VALUES
    ('interaction-reg-001', '2024-01-15T08:00:00+00:00', 'REGISTRATION', 'agent-001'),
    ('interaction-reg-002', '2024-01-20T09:30:00+00:00', 'REGISTRATION', 'agent-002'),
    ('interaction-reg-003', '2024-02-01T10:15:00+00:00', 'REGISTRATION', 'agent-003'),
    ('interaction-reg-004', '2024-02-10T11:00:00+00:00', 'REGISTRATION', 'agent-004'),
    ('interaction-reg-005', '2024-02-25T14:45:00+00:00', 'REGISTRATION', 'agent-005');

-- Ping interactions (regular health checks)
INSERT INTO agent_interaction (id, timestamp, type, agent_id) VALUES
    -- Agent 001 pings (regular, recent)
    ('interaction-ping-001', '2025-10-23T08:00:00+00:00', 'PING', 'agent-001'),
    ('interaction-ping-002', '2025-10-22T08:00:00+00:00', 'PING', 'agent-001'),
    ('interaction-ping-003', '2025-10-21T08:00:00+00:00', 'PING', 'agent-001'),
    ('interaction-ping-004', '2025-10-20T08:00:00+00:00', 'PING', 'agent-001'),
    ('interaction-ping-005', '2025-10-19T08:00:00+00:00', 'PING', 'agent-001'),

    -- Agent 002 pings (regular, recent)
    ('interaction-ping-006', '2025-10-23T10:30:00+00:00', 'PING', 'agent-002'),
    ('interaction-ping-007', '2025-10-22T10:30:00+00:00', 'PING', 'agent-002'),
    ('interaction-ping-008', '2025-10-21T10:30:00+00:00', 'PING', 'agent-002'),
    ('interaction-ping-009', '2025-10-20T10:30:00+00:00', 'PING', 'agent-002'),

    -- Agent 3 pings (old, inactive - over 3 days old)
    ('interaction-ping-010', '2025-10-15T14:00:00+00:00', 'PING', 'agent-003'),
    ('interaction-ping-011', '2025-10-12T14:00:00+00:00', 'PING', 'agent-003'),

    -- Agent 004 pings (very recent)
    ('interaction-ping-012', '2025-10-23T16:45:00+00:00', 'PING', 'agent-004'),
    ('interaction-ping-013', '2025-10-23T12:45:00+00:00', 'PING', 'agent-004'),
    ('interaction-ping-014', '2025-10-22T16:45:00+00:00', 'PING', 'agent-004'),

    -- Agent 005 pings (old - over 3 days old)
    ('interaction-ping-015', '2025-10-18T09:00:00+00:00', 'PING', 'agent-005'),
    ('interaction-ping-016', '2025-10-15T09:00:00+00:00', 'PING', 'agent-005');

-- Report interactions (corresponding to submitted reports)
INSERT INTO agent_interaction (id, timestamp, type, agent_id) VALUES
    ('interaction-report-001', '2024-10-01T09:00:00+00:00', 'REPORT', 'agent-001'),
    ('interaction-report-002', '2024-10-01T14:30:00+00:00', 'REPORT', 'agent-002'),
    ('interaction-report-003', '2024-10-02T08:15:00+00:00', 'REPORT', 'agent-001'),
    ('interaction-report-004', '2024-10-02T16:45:00+00:00', 'REPORT', 'agent-004'),
    ('interaction-report-005', '2024-10-03T10:20:00+00:00', 'REPORT', 'agent-002'),
    ('interaction-report-006', '2024-10-03T13:10:00+00:00', 'REPORT', 'agent-005'),
    ('interaction-report-007', '2024-10-04T11:30:00+00:00', 'REPORT', 'agent-001'),
    ('interaction-report-008', '2024-10-04T15:20:00+00:00', 'REPORT', 'agent-004'),
    ('interaction-report-009', '2024-10-05T09:45:00+00:00', 'REPORT', 'agent-002'),
    ('interaction-report-010', '2024-10-05T14:15:00+00:00', 'REPORT', 'agent-005'),
    ('interaction-report-011', '2024-10-06T10:00:00+00:00', 'REPORT', 'agent-001'),
    ('interaction-report-012', '2024-10-06T16:30:00+00:00', 'REPORT', 'agent-004'),
    ('interaction-report-013', '2024-10-07T08:45:00+00:00', 'REPORT', 'agent-002'),
    ('interaction-report-014', '2024-10-07T12:20:00+00:00', 'REPORT', 'agent-005'),
    ('interaction-report-015', '2024-10-08T11:15:00+00:00', 'REPORT', 'agent-001'),
    ('interaction-report-016', '2024-10-08T15:40:00+00:00', 'REPORT', 'agent-004'),
    ('interaction-report-017', '2024-10-09T09:30:00+00:00', 'REPORT', 'agent-002'),
    ('interaction-report-018', '2024-10-09T14:50:00+00:00', 'REPORT', 'agent-005'),
    ('interaction-report-019', '2024-10-10T10:45:00+00:00', 'REPORT', 'agent-001'),
    ('interaction-report-020', '2024-10-10T16:10:00+00:00', 'REPORT', 'agent-004'),
    ('interaction-report-current-1', NOW() - INTERVAL '2 hours', 'REPORT', 'agent-001'),
    ('interaction-report-current-2', NOW() - INTERVAL '1 hour', 'REPORT', 'agent-002'),
    ('interaction-report-current-3', NOW() - INTERVAL '30 minutes', 'REPORT', 'agent-004');
