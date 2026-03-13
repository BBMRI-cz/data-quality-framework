CREATE TABLE quality_check_keyword
(
    quality_check_hash TEXT NOT NULL,
    keyword            TEXT NOT NULL,
    PRIMARY KEY (quality_check_hash, keyword),
    FOREIGN KEY (quality_check_hash) REFERENCES quality_check (hash) ON DELETE CASCADE,
    CONSTRAINT keyword_length CHECK (CHAR_LENGTH(keyword) <= 250)
);

CREATE INDEX idx_quality_check_keyword_hash ON quality_check_keyword (quality_check_hash);
