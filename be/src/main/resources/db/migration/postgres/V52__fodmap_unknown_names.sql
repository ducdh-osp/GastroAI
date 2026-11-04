CREATE TABLE fodmap_unknown_names (
    id              BIGSERIAL PRIMARY KEY,
    normalized_name VARCHAR(120) NOT NULL,
    sample_name     VARCHAR(120) NOT NULL,
    hit_count       INTEGER      NOT NULL DEFAULT 1,
    dismissed       BOOLEAN      NOT NULL DEFAULT FALSE,
    first_seen_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_seen_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_fodmap_unknown_names_normalized_name UNIQUE (normalized_name)
);

CREATE INDEX idx_fodmap_unknown_names_pending
    ON fodmap_unknown_names(dismissed, hit_count DESC);