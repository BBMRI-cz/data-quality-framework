CREATE TABLE result_patient_ids (
    result_id BIGINT NOT NULL,
    patient_id VARCHAR(255),
    CONSTRAINT fk_result_patient_ids_on_result FOREIGN KEY (result_id) REFERENCES result (id)
);

ALTER TABLE result ADD COLUMN stratum VARCHAR(255);
