CREATE TABLE ar_sessions
(
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id),
    recipe_id  BIGINT       NOT NULL REFERENCES recipes (id),
    post_id    BIGINT       REFERENCES posts (id),
    rating     SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    caption    TEXT,
    photo_url  VARCHAR(512) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ar_sessions_user ON ar_sessions (user_id);
