-- Create report table
CREATE TABLE report
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    generated_at TIMESTAMP,
    epsilon_budget REAL DEFAULT 2.0,
    number_of_entities INTEGER,
    status       VARCHAR(50) DEFAULT 'GENERATING'
);

-- Create cql_check table
CREATE TABLE cql_check
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        VARCHAR(255),
    description VARCHAR(1024),
    query       TEXT,
    warning_threshold INTEGER,
    error_threshold INTEGER,
    epsilon_budget REAL DEFAULT 0.2
);

-- Create result table
CREATE TABLE result
(
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    check_name       VARCHAR(255),
    check_id         INTEGER,
    raw_value        INTEGER,
    obfuscated_value REAL,
    report_id        INTEGER,
    warning_threshold INTEGER,
    error_threshold INTEGER,
    epsilon REAL,
    error VARCHAR(255),
    stratum VARCHAR(255),
    CONSTRAINT fk_result_report FOREIGN KEY (report_id) REFERENCES report(id) ON DELETE CASCADE
);

-- Create result_patient_ids table
CREATE TABLE result_patient_ids (
    result_id INTEGER NOT NULL,
    patient_id VARCHAR(255),
    CONSTRAINT fk_result_patient_ids_on_result FOREIGN KEY (result_id) REFERENCES result (id)
);

-- Create user_account table
CREATE TABLE user_account
(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

-- Create settings table
CREATE TABLE settings (
    setting_name VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(500) NOT NULL
);

-- Create server table
CREATE TABLE server (
    id VARCHAR(255) PRIMARY KEY,
    url VARCHAR(500) NOT NULL UNIQUE,
    name VARCHAR(255),
    client_id VARCHAR(255),
    client_secret VARCHAR(500),
    status VARCHAR(50) NOT NULL
);

-- Create server_interaction table
CREATE TABLE server_interaction (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    server_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    FOREIGN KEY (server_id) REFERENCES server(id) ON DELETE CASCADE
);

-- Create indexes on server_interaction for better query performance
CREATE INDEX idx_server_interaction_server_id ON server_interaction(server_id);
CREATE INDEX idx_server_interaction_timestamp ON server_interaction(timestamp);

-- Insert default user account
INSERT INTO user_account (id, username, password) VALUES
(102,'admin', '$argon2id$v=19$m=19456,t=2,p=1$SQGK8wXpQw5b+qjuq/Ih1A$WP87YsUIErq6O+7rMk65U0cH4OHBRdrnM3yIG50gwpE');

-- Insert default settings
INSERT INTO settings (setting_name, setting_value) VALUES ('fhirUrl', 'http://localhost:8080/fhir');
INSERT INTO settings (setting_name, setting_value) VALUES ('fhirUsername', 'fhiruser');
INSERT INTO settings (setting_name, setting_value) VALUES ('fhirPassword', 'ZmhpcnBhc3M=');
INSERT INTO settings (setting_name, setting_value) VALUES ('agentId',
    LOWER(HEX(RANDOMBLOB(4))) || '-' ||
    LOWER(HEX(RANDOMBLOB(2))) || '-' ||
    LOWER(HEX(RANDOMBLOB(2))) || '-' ||
    LOWER(HEX(RANDOMBLOB(2))) || '-' ||
    LOWER(HEX(RANDOMBLOB(6))));

-- Insert CQL checks
INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES ('Missing Gender attribute',
        'How many patients do not have the Gender attribute',
        'library GenderPresenceCheck version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define HasGender:
   not Patient.gender.exists()

define InInitialPopulation:
  HasGender', 10, 30);

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Impossible birth date (in the future or before 20th century)',
           'How many patients have birth dates before 1900 or in the future',
           'library BirthDateCheck version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define CurrentDate: @2025-05-27

define InInitialPopulation:
  exists [Patient] P
    where P.birthDate < @1900-01-01
      or P.birthDate > CurrentDate',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Gender-Incompatible Diagnosis',
           'How many patients have a diagnosis incompatible with their gender',
           'library "GenderIncompatibleDiagnosis"
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

codesystem icd10: ''http://hl7.org/fhir/sid/icd-10''

code "ProstateCancer": ''C61'' from icd10
code "CervicalCancer": ''R80.2'' from icd10

context Patient

define PatientGender:
  Patient.gender

define MaleSpecificConditions:
  [Condition: code in "ProstateCancer"] C
    where C.subject.reference = ''Patient/'' + Patient.id

define FemaleSpecificConditions:
  [Condition: code in "CervicalCancer"] C
    where C.subject.reference = ''Patient/'' + Patient.id

define GenderIncompatibleDiagnosis:
  (PatientGender = ''female'' and exists MaleSpecificConditions)
    or (PatientGender = ''male'' and exists FemaleSpecificConditions)

define InInitialPopulation:
  exists MaleSpecificConditions
    or exists FemaleSpecificConditions
    or GenderIncompatibleDiagnosis',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Missing diagnosis or condition',
           'How many patients have no diagnosis or a diagnosis without a code',
           'library PatientConditionChecks version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define HasNoCondition:
  not exists (
    [Condition] C
      where C.subject.reference = ''Patient/'' + Patient.id
  )

define HasConditionWithNoCode:
  exists (
    [Condition] C
      where C.subject.reference = ''Patient/'' + Patient.id
        and C.code is null
  )

define InInitialPopulation:
  HasNoCondition or HasConditionWithNoCode',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Unsupported Gender Value',
           'How many patients have unsupported or undefined gender values',
           'library UnsupportedGenderPatients version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define InInitialPopulation:
  Patient.gender is null
    or Patient.gender !~ ''male''
    and Patient.gender !~ ''female''
    and Patient.gender !~ ''other''
    and Patient.gender !~ ''unknown''',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Last update happened more than a year ago',
           'How many patients were last updated in 2025',
           'library PatientsUpdatedIn2025 version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define InInitialPopulation:
  Patient.meta.lastUpdated before @2026-01-01T00:00:00Z',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Patients with condition without onset',
           'How many patients have conditions without onset information',
           'library ConditionMissingOnset version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define ConditionsWithoutOnset:
  [Condition] C
    where C.subject.reference = ''Patient/'' + Patient.id
      and C.onset is null

define InInitialPopulation:
  exists ConditionsWithoutOnset',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Patients without identifier',
           'How many patients do not have at least one identifier',
           'library PatientMissingIdentifier version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define InInitialPopulation:
  not exists Patient.identifier',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Patients without birth date',
           'How many patients do not have a birth date',
           'library PatientMissingBirthDate version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define InInitialPopulation:
  Patient.birthDate is null',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Patients with specimen without collection date',
           'How many patients have specimens without collection date information',
           'library SpecimenMissingCollectionDate version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define SpecimensWithoutCollectionDate:
  [Specimen] S
    where S.subject.reference = ''Patient/'' + Patient.id
      and S.collection.collected is null

define InInitialPopulation:
  exists SpecimensWithoutCollectionDate',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Patients with specimen without storage temperature',
           'How many patients have specimens without storageTemperature extension',
           'library SpecimenMissingStorageTemperature version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define SpecimensWithoutStorageTemperature:
  [Specimen] S
    where S.subject.reference = ''Patient/'' + Patient.id
      and not exists (
        S.extension E
          where E.url = ''https://fhir.bbmri.de/StructureDefinition/StorageTemperature''
      )

define InInitialPopulation:
  exists SpecimensWithoutStorageTemperature',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Patients with specimen without sample diagnosis',
           'How many patients have specimens without SampleDiagnosis extension',
           'library SpecimenMissingSampleDiagnosis version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define SpecimensWithoutSampleDiagnosis:
  [Specimen] S
    where S.subject.reference = ''Patient/'' + Patient.id
      and not exists (
        S.extension E
          where E.url = ''https://fhir.bbmri.de/StructureDefinition/SampleDiagnosis''
      )

define InInitialPopulation:
  exists SpecimensWithoutSampleDiagnosis',
           10, 30
       );

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Patients with specimen without custodian',
           'How many patients have specimens without Custodian extension',
           'library SpecimenMissingCustodian version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define SpecimensWithoutCustodian:
  [Specimen] S
    where S.subject.reference = ''Patient/'' + Patient.id
      and not exists (
        S.extension E
          where E.url = ''https://fhir.bbmri.de/StructureDefinition/Custodian''
      )

define InInitialPopulation:
  exists SpecimensWithoutCustodian',
           10, 30
       );

