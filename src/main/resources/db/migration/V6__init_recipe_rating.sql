CREATE TABLE recipe_ratings
(
    id         BIGSERIAL PRIMARY KEY,
    recipe_id  BIGINT      NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    score      SMALLINT    NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment    VARCHAR(500),
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_recipe_rating UNIQUE (recipe_id, user_id)
);

CREATE INDEX idx_recipe_ratings_user   ON recipe_ratings (user_id);
