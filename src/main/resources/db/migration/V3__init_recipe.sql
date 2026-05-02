CREATE TABLE recipes
(
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255)   NOT NULL,
    description       TEXT,
    thumbnail_url     VARCHAR(512),
    category          VARCHAR(32)    NOT NULL,
    base_spirit       VARCHAR(32),
    difficulty        SMALLINT       NOT NULL DEFAULT 1 CHECK (difficulty BETWEEN 1 AND 3),
    estimated_minutes INT,
    abv               NUMERIC(4, 1),
    average_rating    NUMERIC(3, 2)  NOT NULL DEFAULT 0,
    rating_count      INT            NOT NULL DEFAULT 0,
    is_custom         BOOLEAN        NOT NULL DEFAULT FALSE,
    author_id         BIGINT,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT recipes_custom_author_check
        CHECK ((is_custom = TRUE AND author_id IS NOT NULL) OR (is_custom = FALSE AND author_id IS NULL))
);

CREATE INDEX idx_recipes_is_custom ON recipes (is_custom);
CREATE INDEX idx_recipes_author ON recipes (author_id) WHERE author_id IS NOT NULL;
CREATE INDEX idx_recipes_name ON recipes (name);

CREATE TABLE recipe_taste_tags
(
    recipe_id BIGINT      NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    taste_tag VARCHAR(32) NOT NULL,
    PRIMARY KEY (recipe_id, taste_tag)
);

CREATE TABLE recipe_ingredients
(
    id            BIGSERIAL PRIMARY KEY,
    recipe_id     BIGINT       NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    amount        VARCHAR(32),
    unit          VARCHAR(32),
    display_order SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_recipe_ingredients_recipe ON recipe_ingredients (recipe_id);
CREATE INDEX idx_recipe_ingredients_name ON recipe_ingredients (name);

CREATE TABLE recipe_steps
(
    id          BIGSERIAL PRIMARY KEY,
    recipe_id   BIGINT   NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    step_order  SMALLINT NOT NULL,
    description TEXT     NOT NULL,
    CONSTRAINT recipe_steps_uniq UNIQUE (recipe_id, step_order)
);

CREATE INDEX idx_recipe_steps_recipe ON recipe_steps (recipe_id);
