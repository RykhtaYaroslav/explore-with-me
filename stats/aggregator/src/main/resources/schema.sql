CREATE TABLE IF NOT EXISTS aggregator_user_event_interactions (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    last_interaction_time TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_aggregator_user_event_interactions PRIMARY KEY (user_id, event_id)
);
COMMENT ON TABLE aggregator_user_event_interactions IS
    'Stores each user’s maximum interaction weight and latest interaction time for each event.';

CREATE INDEX IF NOT EXISTS idx_aggregator_interactions_event_user
    ON aggregator_user_event_interactions (event_id, user_id);

CREATE TABLE IF NOT EXISTS aggregator_event_weights (
    event_id BIGINT PRIMARY KEY,
    total_weight DOUBLE PRECISION NOT NULL
);
COMMENT ON TABLE aggregator_event_weights IS
    'Stores the sum of users’ maximum interaction weights for each event, used as a similarity denominator.';

CREATE TABLE IF NOT EXISTS aggregator_event_pair_weights (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    shared_weight DOUBLE PRECISION NOT NULL,
    CONSTRAINT pk_aggregator_event_pair_weights PRIMARY KEY (event_a, event_b),
    CONSTRAINT chk_aggregator_event_pair_order CHECK (event_a < event_b)
);
COMMENT ON TABLE aggregator_event_pair_weights IS
    'Stores the sum of per-user minimum weights shared by each canonical pair of events, used as a similarity numerator.';
