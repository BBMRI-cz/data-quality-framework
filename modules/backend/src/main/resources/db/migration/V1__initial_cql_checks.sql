CREATE TABLE report
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    generated_at TIMESTAMP,
    status       VARCHAR(50) DEFAULT 'GENERATING'
);
CREATE TABLE cql_check
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255),
    description VARCHAR(1024),
    query       CLOB
);
INSERT INTO cql_check (name, description, query)
VALUES ('Gender Presence Check',
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
$$);
