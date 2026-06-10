
INSERT INTO report (id, generated_at, epsilon_budget, number_of_entities, number_of_secondary_entities, status)
VALUES
    (1001, '2023-10-25 10:00:00', 2.0, 1500, 3000, 'GENERATED'),
    (1002, '2023-10-26 14:30:00', 2.0, 1550, 3100, 'GENERATED'),
    (1003, '2023-10-27 09:15:00', 2.0, 1500, null, 'GENERATED'),
    (1004, '2023-10-28 12:00:00', 2.0, null, null, 'GENERATED');

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

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, error, stratum)
SELECT name, id, NULL, NULL, 1002, warning_threshold, error_threshold, epsilon_budget, 'Null result encountered during aggregation', NULL FROM quality_check WHERE name = 'Patients without identifier';


-- Report 1003 Results (Partial results for testing)
INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 10, 10.5, 1003, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Missing Gender attribute';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 5, 5.2, 1003, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Impossible birth date (in the future or before 20th century)';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, stratum)
SELECT name, id, 20, 19.8, 1003, warning_threshold, error_threshold, epsilon_budget, NULL FROM quality_check WHERE name = 'Missing diagnosis or condition';


-- Report 1004 Results (Null results with an explicit error value)
INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, error, stratum)
SELECT name, id, NULL, NULL, 1004, warning_threshold, error_threshold, epsilon_budget, 'Calculation failed while processing null input', NULL FROM quality_check WHERE name = 'Missing Gender attribute';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, error, stratum)
SELECT name, id, NULL, NULL, 1004, warning_threshold, error_threshold, epsilon_budget, NULL, NULL FROM quality_check WHERE name = 'Impossible birth date (in the future or before 20th century)';

INSERT INTO result (check_name, check_id, raw_value, obfuscated_value, report_id, warning_threshold, error_threshold, epsilon, error, stratum)
SELECT name, id, NULL, NULL, 1004, warning_threshold, error_threshold, epsilon_budget, NULL, NULL FROM quality_check WHERE name = 'Missing diagnosis or condition';

-- Add 10 basic OMOP CDM quality checks
INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons with missing birth year',
     'Persons with missing or null year_of_birth in the OMOP PERSON table',
     'SELECT person_id FROM person WHERE year_of_birth IS NULL',
     'SQL', 10, 50, 0.2);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons with invalid gender concept',
     'Persons with a gender_concept_id that is not a valid OMOP standard gender concept',
     'SELECT person_id FROM person WHERE gender_concept_id NOT IN (8507, 8532, 8521, 8570, 8551) OR gender_concept_id IS NULL',
     'SQL', 5, 20, 0.2);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons with conditions without concept',
     'Persons who have at least one condition_occurrence record with missing or zero condition_concept_id',
     'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM condition_occurrence c WHERE c.person_id = p.person_id AND (c.condition_concept_id IS NULL OR c.condition_concept_id = 0))',
     'SQL', 10, 100, 0.2);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons with measurements without result',
     'Persons who have at least one measurement record with missing result (value_as_number IS NULL AND value_as_concept_id IS NULL)',
     'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM measurement m WHERE m.person_id = p.person_id AND m.value_as_number IS NULL AND m.value_as_concept_id IS NULL)',
     'SQL', 20, 100, 0.2);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons with drug exposures without concept',
     'Persons who have at least one drug_exposure record with missing or zero drug_concept_id',
     'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM drug_exposure d WHERE d.person_id = p.person_id AND (d.drug_concept_id IS NULL OR d.drug_concept_id = 0))',
     'SQL', 10, 50, 0.2);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons with visits without end date',
     'Persons who have at least one visit_occurrence record with missing visit_end_date',
     'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM visit_occurrence v WHERE v.person_id = p.person_id AND v.visit_end_date IS NULL)',
     'SQL', 15, 50, 0.2);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons with observations without date',
     'Persons who have at least one observation record with missing observation_date',
     'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM observation o WHERE o.person_id = p.person_id AND o.observation_date IS NULL)',
     'SQL', 10, 50, 0.2);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons with death records without date',
     'Persons who have a death record with missing death_date',
     'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM death d WHERE d.person_id = p.person_id AND d.death_date IS NULL)',
     'SQL', 1, 5, 0.2);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons with procedures without concept',
     'Persons who have at least one procedure_occurrence record with missing or zero procedure_concept_id',
     'SELECT person_id FROM person p WHERE EXISTS (SELECT 1 FROM procedure_occurrence pr WHERE pr.person_id = p.person_id AND (pr.procedure_concept_id IS NULL OR pr.procedure_concept_id = 0))',
     'SQL', 10, 50, 0.2);

INSERT INTO quality_check (name, description, query, check_type, warning_threshold, error_threshold, epsilon_budget)
VALUES
    ('OMOP: Persons without visits',
     'Persons who have no associated visit_occurrence record',
     'SELECT person_id FROM person WHERE person_id NOT IN (SELECT person_id FROM visit_occurrence)',
     'SQL', 5, 30, 0.2);

-- Default SQL connection settings for OMOP development
INSERT OR REPLACE INTO settings (setting_name, setting_value) VALUES
    ('sqlUrl', 'jdbc:postgresql://localhost:5432/omop'),
    ('sqlUsername', 'postgres'),
    ('sqlPassword', 'cG9zdGdyZXM=');
