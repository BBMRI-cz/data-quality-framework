INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES ('Missing Gender attribute',
        'How many patients do not have the Gender attribute',
        $$
library GenderPresenceCheck version '1.0.0'
using FHIR version '4.0.0'
include FHIRHelpers version '4.0.0'

context Patient

define HasGender:
   not Patient.gender.exists()

define InInitialPopulation:
  HasGender
$$, 10, 30);

-- Birth Date Check
INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Impossible birth date (in the future or before 20th century)',
           'How many patients have birth dates before 1900 or in the future',
           $$
library BirthDateCheck version '1.0.0'
using FHIR version '4.0.0'
include FHIRHelpers version '4.0.0'

context Patient

define CurrentDate: @2025-05-27

define InInitialPopulation:
  exists [Patient] P
    where P.birthDate < @1900-01-01
      or P.birthDate > CurrentDate
$$,
           10, 30
       );

-- Gender-Incompatible Diagnosis Check
INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Gender-Incompatible Diagnosis',
           'How many patients have a diagnosis incompatible with their gender',
           $$
library "GenderIncompatibleDiagnosis"
using FHIR version '4.0.0'
include FHIRHelpers version '4.0.0'

codesystem icd10: 'http://hl7.org/fhir/sid/icd-10'

code "ProstateCancer": 'C61' from icd10
code "CervicalCancer": 'R80.2' from icd10

context Patient

define PatientGender:
  Patient.gender

define MaleSpecificConditions:
  [Condition: code in "ProstateCancer"] C
    where C.subject.reference = 'Patient/' + Patient.id

define FemaleSpecificConditions:
  [Condition: code in "CervicalCancer"] C
    where C.subject.reference = 'Patient/' + Patient.id

define GenderIncompatibleDiagnosis:
  (PatientGender = 'female' and exists MaleSpecificConditions)
    or (PatientGender = 'male' and exists FemaleSpecificConditions)

define InInitialPopulation:
  exists MaleSpecificConditions
    or exists FemaleSpecificConditions
    or GenderIncompatibleDiagnosis
$$,
           10, 30
       );

-- Condition Coverage Check
INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Missing diagnosis or condition',
           'How many patients have no diagnosis or a diagnosis without a code',
           $$
library PatientConditionChecks version '1.0.0'
using FHIR version '4.0.0'
include FHIRHelpers version '4.0.0'

context Patient

define HasNoCondition:
  not exists (
    [Condition] C
      where C.subject.reference = 'Patient/' + Patient.id
  )

define HasConditionWithNoCode:
  exists (
    [Condition] C
      where C.subject.reference = 'Patient/' + Patient.id
        and C.code is null
  )

define InInitialPopulation:
  HasNoCondition or HasConditionWithNoCode
$$,
           10, 30
       );

-- Unsupported Gender Value Check
INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Unsupported Gender Value',
           'How many patients have unsupported or undefined gender values',
           $$
library UnsupportedGenderPatients version '1.0.0'
using FHIR version '4.0.0'
include FHIRHelpers version '4.0.0'

context Patient

define InInitialPopulation:
  Patient.gender is null
    or Patient.gender !~ 'male'
    and Patient.gender !~ 'female'
    and Patient.gender !~ 'other'
    and Patient.gender !~ 'unknown'
$$,
           10, 30
       );

-- Patients Updated in 2025
INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Last update happened more than a year ago',
           'How many patients were last updated in 2025',
           $$
library PatientsUpdatedIn2025 version '1.0.0'
using FHIR version '4.0.0'
include FHIRHelpers version '4.0.0'

context Patient

define InInitialPopulation:
  Patient.meta.lastUpdated before @2026-01-01T00:00:00Z
$$,
           10, 30
       );
