
INSERT INTO report (id, generated_at, epsilon_budget, number_of_entities, number_of_secondary_entities, status)
VALUES
    (1001, '2023-10-25 10:00:00', 2.0, 1500, 3000, 'GENERATED'),
    (1002, '2023-10-26 14:30:00', 2.0, 1550, 3100, 'GENERATED'),
    (1003, '2023-10-27 09:15:00', 2.0, 1500, null, 'GENERATED');

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 5, 5.2, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Missing Gender attribute';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 2, 2.1, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Impossible birth date (in the future or before 20th century)';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 0, 0.0, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Gender-Incompatible Diagnosis';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 15, 14.8, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Missing diagnosis or condition';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 1, 1.1, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Unsupported Gender Value';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 8, 8.3, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with condition without onset';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 3, 3.0, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients without identifier';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 12, 11.9, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients without birth date';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 20, 20.5, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with specimen without collection date';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 5, 5.1, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with specimen without storage temperature';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 0, 0.0, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with specimen without sample diagnosis';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 40, 40.2, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with specimen without custodian';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 6, 6.0, 1001, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients without specimen';


-- Report 1002 Results (Slightly different values)
INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 4, 4.2, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Missing Gender attribute';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 2, 2.0, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Impossible birth date (in the future or before 20th century)';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 1, 1.2, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Gender-Incompatible Diagnosis';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 12, 12.5, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Missing diagnosis or condition';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 0, 0.0, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Unsupported Gender Value';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 7, 7.3, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with condition without onset';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 2, 2.1, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients without identifier';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 10, 9.9, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients without birth date';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 18, 18.2, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with specimen without collection date';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 6, 6.1, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with specimen without storage temperature';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 0, 0.0, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with specimen without sample diagnosis';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 35, 35.1, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients with specimen without custodian';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 5, 5.0, 1002, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Patients without specimen';


-- Report 1003 Results (Partial results for testing)
INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 10, 10.5, 1003, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Missing Gender attribute';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 5, 5.2, 1003, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Impossible birth date (in the future or before 20th century)';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 20, 19.8, 1003, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Missing diagnosis or condition';
