CREATE TABLE patient
(
    id VARCHAR(255) NOT NULL,
    CONSTRAINT pk_patient PRIMARY KEY (id)
);

CREATE TABLE result_patients
(
    result_id   BIGINT       NOT NULL,
    patient_id VARCHAR(255) NOT NULL
);

ALTER TABLE result ADD COLUMN stratum VARCHAR(255);

ALTER TABLE result_patients
    ADD CONSTRAINT fk_respat_on_patient FOREIGN KEY (patient_id) REFERENCES patient (id);

ALTER TABLE result_patients
    ADD CONSTRAINT fk_respat_on_result FOREIGN KEY (result_id) REFERENCES result (id);