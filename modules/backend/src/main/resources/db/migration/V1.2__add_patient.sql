CREATE TABLE result_patient_ids (
    result_id BIGINT NOT NULL,
    patient_id VARCHAR(255),
    CONSTRAINT fk_result_patient_ids_on_result FOREIGN KEY (result_id) REFERENCES result (id)
);

ALTER TABLE result ADD COLUMN stratum VARCHAR(255);

CREATE TABLE user_account
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

INSERT INTO user_account (id, username, password) VALUES
(102,'admin', '$argon2id$v=19$m=65536,t=4,p=1$78FCaY3kUYtDS88P5X/GGg$K6eyKS2j7DbHlYmUlK+zsR9i30D6WCRcYSiEdZoWiXA');
