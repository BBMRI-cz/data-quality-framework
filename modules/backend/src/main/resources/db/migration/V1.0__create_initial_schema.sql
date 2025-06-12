CREATE TABLE report
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    generated_at TIMESTAMP,
    epsilon_budget float default 2.0,
    number_of_entities int,
    status       VARCHAR(50) DEFAULT 'GENERATING'
);

CREATE TABLE cql_check
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255),
    description VARCHAR(1024),
    query       CLOB,
    warning_threshold int,
    error_threshold int,
    epsilon_budget float default 0.2

);

CREATE TABLE result
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY, -- Changed from VARCHAR(255) to BIGINT
    check_name       VARCHAR(255),
    check_id         BIGINT,
    raw_value            INT,
    obfuscated_value INT,
    report_id        BIGINT,
    warning_threshold int,
    error_threshold int,
    epsilon float,
    error VARCHAR(255),
    CONSTRAINT fk_result_report FOREIGN KEY (report_id) REFERENCES report(id) ON DELETE CASCADE
);