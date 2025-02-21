CREATE TABLE IF NOT EXISTS session_events (
                                              id SERIAL PRIMARY KEY,
                                              session_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    startup_time_ms BIGINT,
    buffering_duration_ms BIGINT,
    bitrate INTEGER,
    buffering_ratio DOUBLE PRECISION,
    error_count INTEGER,
    device_type VARCHAR(255),
    region VARCHAR(255),
    content_id VARCHAR(255)
    );

CREATE INDEX IF NOT EXISTS idx_session_events_session_id ON session_events(session_id);
CREATE INDEX IF NOT EXISTS idx_session_events_timestamp ON session_events(timestamp);