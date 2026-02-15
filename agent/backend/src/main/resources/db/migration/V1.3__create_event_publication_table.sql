-- Create event_publication table for Spring Modulith JPA event publication registry
CREATE TABLE event_publication (
    id VARCHAR(36) PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP NOT NULL,
    completion_date TIMESTAMP
);

-- Create index for faster lookup of incomplete publications
CREATE INDEX idx_event_publication_completion_date ON event_publication(completion_date);


