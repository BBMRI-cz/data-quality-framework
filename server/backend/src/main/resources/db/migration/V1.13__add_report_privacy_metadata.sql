ALTER TABLE report
    ADD COLUMN agent_version                   TEXT,
    ADD COLUMN privacy_mechanism               TEXT,
    ADD COLUMN privacy_epsilon                 DOUBLE PRECISION,
    ADD COLUMN privacy_delta                   DOUBLE PRECISION,
    ADD COLUMN privacy_min_threshold           INTEGER,
    ADD COLUMN privacy_check_results_epsilon   DOUBLE PRECISION,
    ADD COLUMN privacy_entity_total_epsilon    DOUBLE PRECISION,
    ADD COLUMN privacy_entity_total_mechanism  TEXT,
    ADD COLUMN total_patients_epsilon          DOUBLE PRECISION,
    ADD COLUMN total_patients_sensitivity      INTEGER,
    ADD COLUMN total_samples_epsilon           DOUBLE PRECISION,
    ADD COLUMN total_samples_sensitivity       INTEGER;

ALTER TABLE quality_check_result
    ADD COLUMN epsilon     DOUBLE PRECISION,
    ADD COLUMN sensitivity INTEGER;
