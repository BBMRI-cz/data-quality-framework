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
(102,'admin', '$argon2id$v=19$m=19456,t=2,p=1$SQGK8wXpQw5b+qjuq/Ih1A$WP87YsUIErq6O+7rMk65U0cH4OHBRdrnM3yIG50gwpE');

CREATE TABLE event_publication
(
    id               UUID NOT NULL,
    completion_date  TIMESTAMP,
    event_type       VARCHAR(512) NOT NULL,
    listener_id      VARCHAR(512) NOT NULL,
    publication_date TIMESTAMP NOT NULL,
    serialized_event VARCHAR(4000) NOT NULL,
    PRIMARY KEY (id)
);
