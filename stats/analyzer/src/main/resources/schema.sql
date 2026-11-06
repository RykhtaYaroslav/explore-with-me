CREATE TABLE IF NOT EXISTS user_event_interactions (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    last_interaction_time TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_user_event_interactions PRIMARY KEY (user_id, event_id)
);
COMMENT ON TABLE user_event_interactions IS
    'Stores each user’s maximum interaction weight and latest interaction time for each event.';

CREATE TABLE IF NOT EXISTS event_similarities (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_event_similarities PRIMARY KEY (event_a, event_b),
    CONSTRAINT chk_event_similarity_order CHECK (event_a < event_b),
    CONSTRAINT chk_event_similarity_score CHECK (score >= 0.0 AND score <= 1.0)
);
COMMENT ON TABLE event_similarities IS
    'Stores the latest similarity score calculated for each canonical pair of events.';
