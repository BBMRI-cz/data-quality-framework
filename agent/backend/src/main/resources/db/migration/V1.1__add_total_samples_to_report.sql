ALTER TABLE report ADD COLUMN number_of_secondary_entities INTEGER ;

DELETE FROM cql_check WHERE name = 'Last update happened more than a year ago';

INSERT INTO cql_check (name, description, query, warning_threshold, error_threshold)
VALUES (
           'Patients without specimen',
           'How many patients do not have any specimens',
           'library PatientWithoutSpecimen version ''1.0.0''
using FHIR version ''4.0.0''
include FHIRHelpers version ''4.0.0''

context Patient

define HasNoSpecimen:
  not exists (
    [Specimen] S
      where S.subject.reference = ''Patient/'' + Patient.id
  )

define InInitialPopulation:
  HasNoSpecimen',
           10, 30
       );

