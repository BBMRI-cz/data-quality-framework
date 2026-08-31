ALTER TABLE quality_check_result DROP CONSTRAINT quality_check_result_quality_check_hash_fkey;
ALTER TABLE quality_check_keyword DROP CONSTRAINT quality_check_keyword_quality_check_hash_fkey;

-- quality_check: add sequential id, swap PK, keep hash unique
ALTER TABLE quality_check ADD COLUMN id BIGSERIAL;
ALTER TABLE quality_check DROP CONSTRAINT quality_check_pkey;
ALTER TABLE quality_check ADD CONSTRAINT pk_quality_check PRIMARY KEY (id);
ALTER TABLE quality_check ADD CONSTRAINT uq_quality_check_hash UNIQUE (hash);

-- quality_check_result: backfill, drop hash column, FK on new id
ALTER TABLE quality_check_result ADD COLUMN quality_check_id BIGINT;
UPDATE quality_check_result r SET quality_check_id = q.id FROM quality_check q WHERE q.hash = r.quality_check_hash;
ALTER TABLE quality_check_result ALTER COLUMN quality_check_id SET NOT NULL;
ALTER TABLE quality_check_result DROP COLUMN quality_check_hash;
ALTER TABLE quality_check_result ADD CONSTRAINT fk_result_quality_check FOREIGN KEY (quality_check_id) REFERENCES quality_check (id) ON DELETE CASCADE;

-- quality_check_keyword: backfill, rebuild PK on id, drop hash
ALTER TABLE quality_check_keyword ADD COLUMN quality_check_id BIGINT;
UPDATE quality_check_keyword k SET quality_check_id = q.id FROM quality_check q WHERE q.hash = k.quality_check_hash;
ALTER TABLE quality_check_keyword ALTER COLUMN quality_check_id SET NOT NULL;
ALTER TABLE quality_check_keyword DROP CONSTRAINT quality_check_keyword_pkey;
ALTER TABLE quality_check_keyword DROP COLUMN quality_check_hash;
ALTER TABLE quality_check_keyword ADD PRIMARY KEY (quality_check_id, keyword);
ALTER TABLE quality_check_keyword ADD CONSTRAINT fk_keyword_quality_check FOREIGN KEY (quality_check_id) REFERENCES quality_check (id) ON DELETE CASCADE;
DROP INDEX IF EXISTS idx_quality_check_keyword_hash;
CREATE INDEX idx_quality_check_keyword_id ON quality_check_keyword (quality_check_id);
