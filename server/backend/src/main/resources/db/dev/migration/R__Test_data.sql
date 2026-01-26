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
INSERT INTO quality_check (hash, name, description, registered_at, warning_threshold, error_threshold, category_id) VALUES
    ('unsupported-gender-check', 'Unsupported Gender Values', 'Percentage of patients with non-supported gender attribute values (not Male/Female/Other/Unknown)', '2024-01-15 10:00:00', 5.0, 15.0, 2),
    ('missing-birthdate-check', 'Missing Birth Date', 'Percentage of patients with missing or null birth date values', '2024-01-15 10:15:00', 3.0, 10.0, 1),
    ('invalid-date-check', 'Invalid Date Values', 'Percentage of records with logically invalid dates (e.g., future birth dates, death before birth)', '2024-01-15 10:30:00', 2.0, 8.0, 2),
    ('duplicate-patient-check', 'Duplicate Patient Records', 'Percentage of patients that appear to be duplicates based on matching identifiers', '2024-01-15 10:45:00', 1.0, 5.0, 3),
    ('invalid-format-check', 'Invalid Data Formats', 'Percentage of records with data that does not follow expected formats (e.g., malformed IDs, invalid postal codes)', '2024-01-15 11:00:00', 4.0, 12.0, NULL),
    ('broken-reference-check', 'Broken Reference Integrity', 'Percentage of records with references to non-existent related entities', '2024-01-15 11:15:00', 2.0, 8.0, 1),
    ('outlier-value-check', 'Statistical Outlier Values', 'Percentage of numerical values that are statistical outliers (e.g., age > 150, negative measurements)', '2024-01-15 11:30:00', 10.0, 25.0, NULL),
    ('invalid-coding-check', 'Invalid Medical Codes', 'Percentage of records with invalid or non-standard medical codes (ICD-10, SNOMED CT)', '2024-01-15 11:45:00', 3.0, 10.0, 2);

-- Insert dummy reports for the past 30 days
-- Some reports include total_patients and total_samples, others are NULL for backwards compatibility testing
INSERT INTO report (id, timestamp, agent_id, total_patients, total_samples) VALUES
    ('report-001', '2024-10-01 09:00:00', 'agent-001', 1250, 3800),
    ('report-002', '2024-10-01 14:30:00', 'agent-002', 890, 2450),
    ('report-003', '2024-10-02 08:15:00', 'agent-001', NULL, NULL),  -- backwards compatibility test
    ('report-004', '2024-10-02 16:45:00', 'agent-004', 2100, 6500),
    ('report-005', '2024-10-03 10:20:00', 'agent-002', 920, 2600),
    ('report-006', '2024-10-03 13:10:00', 'agent-005', NULL, NULL),  -- backwards compatibility test
    ('report-007', '2024-10-04 11:30:00', 'agent-001', 1300, 3950),
    ('report-008', '2024-10-04 15:20:00', 'agent-004', 2150, 6700),
    ('report-009', '2024-10-05 09:45:00', 'agent-002', NULL, NULL),  -- backwards compatibility test
    ('report-010', '2024-10-05 14:15:00', 'agent-005', 1680, 5200),
    ('report-011', '2024-10-06 10:00:00', 'agent-001', 1280, 3900),
    ('report-012', '2024-10-06 16:30:00', 'agent-004', NULL, NULL),  -- backwards compatibility test
    ('report-013', '2024-10-07 08:45:00', 'agent-002', 905, 2520),
    ('report-014', '2024-10-07 12:20:00', 'agent-005', 1700, 5300),
    ('report-015', '2024-10-08 11:15:00', 'agent-001', 1310, 4000),
    ('report-016', '2024-10-08 15:40:00', 'agent-004', 2200, 6850),
    ('report-017', '2024-10-09 09:30:00', 'agent-002', NULL, NULL),  -- backwards compatibility test
    ('report-018', '2024-10-09 14:50:00', 'agent-005', 1720, 5400),
    ('report-019', '2024-10-10 10:45:00', 'agent-001', 1290, 3920),
    ('report-020', '2024-10-10 16:10:00', 'agent-004', 2180, 6750);

-- Insert quality check results for the reports
-- Report 1 results (good quality data - low error rates)
INSERT INTO quality_check_result (report_id, quality_check_hash, result) VALUES
    ('report-001', 'unsupported-gender-check', 0.02),
    ('report-001', 'missing-birthdate-check', 0.01),
    ('report-001', 'invalid-date-check', 0.01),
    ('report-001', 'duplicate-patient-check', 0.0),
    ('report-001', 'invalid-format-check', 0.03),
    ('report-001', 'broken-reference-check', 0.01),
    ('report-001', 'outlier-value-check', 0.08),
    ('report-001', 'invalid-coding-check', 0.02);

-- Report 2 results (mixed quality - some warnings)
INSERT INTO quality_check_result (report_id, quality_check_hash, result) VALUES
    ('report-002', 'unsupported-gender-check', 0.07),
    ('report-002', 'missing-birthdate-check', 0.05),
    ('report-002', 'invalid-date-check', 0.03),
    ('report-002', 'duplicate-patient-check', 0.02),
    ('report-002', 'invalid-format-check', 0.09),
    ('report-002', 'broken-reference-check', 0.04),
    ('report-002', 'outlier-value-check', 0.15),
    ('report-002', 'invalid-coding-check', 0.06);

-- Report 3 results (poor quality data - many errors above thresholds)
INSERT INTO quality_check_result (report_id, quality_check_hash, result) VALUES
    ('report-003', 'unsupported-gender-check', 0.18),
    ('report-003', 'missing-birthdate-check', 0.12),
    ('report-003', 'invalid-date-check', 0.09),
    ('report-003', 'duplicate-patient-check', 0.06),
    ('report-003', 'invalid-format-check', 0.14),
    ('report-003', 'broken-reference-check', 0.11),
    ('report-003', 'outlier-value-check', 0.28),
    ('report-003', 'invalid-coding-check', 0.13);

-- Report 4 results (excellent quality - very low error rates)
INSERT INTO quality_check_result (report_id, quality_check_hash, result) VALUES
    ('report-004', 'unsupported-gender-check', 0.0),
    ('report-004', 'missing-birthdate-check', 0.01),
    ('report-004', 'invalid-date-check', 0.0),
    ('report-004', 'duplicate-patient-check', 0.0),
    ('report-004', 'invalid-format-check', 0.01),
    ('report-004', 'broken-reference-check', 0.0),
    ('report-004', 'outlier-value-check', 0.05),
    ('report-004', 'invalid-coding-check', 0.01);

-- Report 5 results (average quality)
INSERT INTO quality_check_result (report_id, quality_check_hash, result) VALUES
    ('report-005', 'unsupported-gender-check', 0.06),
    ('report-005', 'missing-birthdate-check', 0.04),
    ('report-005', 'invalid-date-check', 0.03),
    ('report-005', 'duplicate-patient-check', 0.02),
    ('report-005', 'invalid-format-check', 0.07),
    ('report-005', 'broken-reference-check', 0.03),
    ('report-005', 'outlier-value-check', 0.12),
    ('report-005', 'invalid-coding-check', 0.05);

-- Add more results for remaining reports with varying quality scores
INSERT INTO quality_check_result (report_id, quality_check_hash, result) VALUES
    -- Report 6 (good quality)
    ('report-006', 'unsupported-gender-check', 0.02),
    ('report-006', 'missing-birthdate-check', 0.02),
    ('report-006', 'invalid-date-check', 0.01),
    ('report-006', 'duplicate-patient-check', 0.01),

    -- Report 7 (poor quality - warnings)
    ('report-007', 'unsupported-gender-check', 0.11),
    ('report-007', 'missing-birthdate-check', 0.08),
    ('report-007', 'invalid-format-check', 0.10),
    ('report-007', 'outlier-value-check', 0.18),

    -- Report 8 (good quality)
    ('report-008', 'unsupported-gender-check', 0.03),
    ('report-008', 'missing-birthdate-check', 0.02),
    ('report-008', 'invalid-date-check', 0.02),
    ('report-008', 'duplicate-patient-check', 0.01),
    ('report-008', 'broken-reference-check', 0.02),

    -- Report 9 (very poor quality - many errors)
    ('report-009', 'unsupported-gender-check', 0.19),
    ('report-009', 'missing-birthdate-check', 0.15),
    ('report-009', 'invalid-format-check', 0.16),
    ('report-009', 'invalid-coding-check', 0.14),

    -- Report 10 (excellent quality)
    ('report-010', 'unsupported-gender-check', 0.01),
    ('report-010', 'missing-birthdate-check', 0.01),
    ('report-010', 'invalid-date-check', 0.01),
    ('report-010', 'duplicate-patient-check', 0.0),
    ('report-010', 'invalid-format-check', 0.02),
    ('report-010', 'broken-reference-check', 0.01),
    ('report-010', 'outlier-value-check', 0.06),
    ('report-010', 'invalid-coding-check', 0.02);

-- Add some recent reports with current timestamps for immediate testing
INSERT INTO report (id, timestamp, agent_id, total_patients, total_samples) VALUES
    ('report-current-1', datetime('now', '-2 hours'), 'agent-001', 1275, 3850),
    ('report-current-2', datetime('now', '-1 hour'), 'agent-002', 895, 2475),
    ('report-current-3', datetime('now', '-30 minutes'), 'agent-004', 2125, 6600);

-- Add results for current reports
INSERT INTO quality_check_result (report_id, quality_check_hash, result) VALUES
    ('report-current-1', 'unsupported-gender-check', 0.04),
    ('report-current-1', 'missing-birthdate-check', 0.02),
    ('report-current-1', 'duplicate-patient-check', 0.01),
    ('report-current-2', 'unsupported-gender-check', 0.09),
    ('report-current-2', 'invalid-format-check', 0.11),
    ('report-current-2', 'invalid-coding-check', 0.08),
    ('report-current-3', 'unsupported-gender-check', 0.01),
    ('report-current-3', 'missing-birthdate-check', 0.02),
    ('report-current-3', 'invalid-date-check', 0.02),
    ('report-current-3', 'broken-reference-check', 0.01);

-- Insert agent interactions
-- Registration interactions (when agents first joined)
INSERT INTO agent_interaction (id, timestamp, type, agent_id) VALUES
    ('interaction-reg-001', '2024-01-15 08:00:00', 'REGISTRATION', 'agent-001'),
    ('interaction-reg-002', '2024-01-20 09:30:00', 'REGISTRATION', 'agent-002'),
    ('interaction-reg-003', '2024-02-01 10:15:00', 'REGISTRATION', 'agent-003'),
    ('interaction-reg-004', '2024-02-10 11:00:00', 'REGISTRATION', 'agent-004'),
    ('interaction-reg-005', '2024-02-25 14:45:00', 'REGISTRATION', 'agent-005');

-- Ping interactions (regular health checks)
INSERT INTO agent_interaction (id, timestamp, type, agent_id) VALUES
    -- Agent 001 pings (regular, recent)
    ('interaction-ping-001', '2025-10-23 08:00:00', 'PING', 'agent-001'),
    ('interaction-ping-002', '2025-10-22 08:00:00', 'PING', 'agent-001'),
    ('interaction-ping-003', '2025-10-21 08:00:00', 'PING', 'agent-001'),
    ('interaction-ping-004', '2025-10-20 08:00:00', 'PING', 'agent-001'),
    ('interaction-ping-005', '2025-10-19 08:00:00', 'PING', 'agent-001'),

    -- Agent 002 pings (regular, recent)
    ('interaction-ping-006', '2025-10-23 10:30:00', 'PING', 'agent-002'),
    ('interaction-ping-007', '2025-10-22 10:30:00', 'PING', 'agent-002'),
    ('interaction-ping-008', '2025-10-21 10:30:00', 'PING', 'agent-002'),
    ('interaction-ping-009', '2025-10-20 10:30:00', 'PING', 'agent-002'),

    -- Agent 3 pings (old, inactive - over 3 days old)
    ('interaction-ping-010', '2025-10-15 14:00:00', 'PING', 'agent-003'),
    ('interaction-ping-011', '2025-10-12 14:00:00', 'PING', 'agent-003'),

    -- Agent 004 pings (very recent)
    ('interaction-ping-012', '2025-10-23 16:45:00', 'PING', 'agent-004'),
    ('interaction-ping-013', '2025-10-23 12:45:00', 'PING', 'agent-004'),
    ('interaction-ping-014', '2025-10-22 16:45:00', 'PING', 'agent-004'),

    -- Agent 005 pings (old - over 3 days old)
    ('interaction-ping-015', '2025-10-18 09:00:00', 'PING', 'agent-005'),
    ('interaction-ping-016', '2025-10-15 09:00:00', 'PING', 'agent-005');

-- Report interactions (corresponding to submitted reports)
INSERT INTO agent_interaction (id, timestamp, type, agent_id) VALUES
    ('interaction-report-001', '2024-10-01 09:00:00', 'REPORT', 'agent-001'),
    ('interaction-report-002', '2024-10-01 14:30:00', 'REPORT', 'agent-002'),
    ('interaction-report-003', '2024-10-02 08:15:00', 'REPORT', 'agent-001'),
    ('interaction-report-004', '2024-10-02 16:45:00', 'REPORT', 'agent-004'),
    ('interaction-report-005', '2024-10-03 10:20:00', 'REPORT', 'agent-002'),
    ('interaction-report-006', '2024-10-03 13:10:00', 'REPORT', 'agent-005'),
    ('interaction-report-007', '2024-10-04 11:30:00', 'REPORT', 'agent-001'),
    ('interaction-report-008', '2024-10-04 15:20:00', 'REPORT', 'agent-004'),
    ('interaction-report-009', '2024-10-05 09:45:00', 'REPORT', 'agent-002'),
    ('interaction-report-010', '2024-10-05 14:15:00', 'REPORT', 'agent-005'),
    ('interaction-report-011', '2024-10-06 10:00:00', 'REPORT', 'agent-001'),
    ('interaction-report-012', '2024-10-06 16:30:00', 'REPORT', 'agent-004'),
    ('interaction-report-013', '2024-10-07 08:45:00', 'REPORT', 'agent-002'),
    ('interaction-report-014', '2024-10-07 12:20:00', 'REPORT', 'agent-005'),
    ('interaction-report-015', '2024-10-08 11:15:00', 'REPORT', 'agent-001'),
    ('interaction-report-016', '2024-10-08 15:40:00', 'REPORT', 'agent-004'),
    ('interaction-report-017', '2024-10-09 09:30:00', 'REPORT', 'agent-002'),
    ('interaction-report-018', '2024-10-09 14:50:00', 'REPORT', 'agent-005'),
    ('interaction-report-019', '2024-10-10 10:45:00', 'REPORT', 'agent-001'),
    ('interaction-report-020', '2024-10-10 16:10:00', 'REPORT', 'agent-004'),
    ('interaction-report-current-1', datetime('now', '-2 hours'), 'REPORT', 'agent-001'),
    ('interaction-report-current-2', datetime('now', '-1 hour'), 'REPORT', 'agent-002'),
    ('interaction-report-current-3', datetime('now', '-30 minutes'), 'REPORT', 'agent-004');
