CREATE TABLE posts
(
    id               BIGSERIAL     PRIMARY KEY,
    author_id        BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id        BIGINT        REFERENCES recipes (id) ON DELETE SET NULL,
    type             VARCHAR(10)   NOT NULL,
    caption          TEXT,
    is_ar_generated  BOOLEAN       NOT NULL DEFAULT FALSE,
    ar_rating        SMALLINT      CHECK (ar_rating BETWEEN 1 AND 5),
    like_count       INT           NOT NULL DEFAULT 0,
    comment_count    INT           NOT NULL DEFAULT 0,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_author      ON posts (author_id);
CREATE INDEX idx_posts_created_at  ON posts (created_at DESC);
CREATE INDEX idx_posts_popularity  ON posts (like_count DESC, comment_count DESC, created_at DESC);

CREATE TABLE post_images
(
    id            BIGSERIAL     PRIMARY KEY,
    post_id       BIGINT        NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    image_url     VARCHAR(512)  NOT NULL,
    display_order INT           NOT NULL DEFAULT 0
);

CREATE INDEX idx_post_images_post ON post_images (post_id);

CREATE TABLE post_likes
(
    id         BIGSERIAL  PRIMARY KEY,
    post_id    BIGINT     NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    user_id    BIGINT     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_post_like UNIQUE (post_id, user_id)
);

CREATE INDEX idx_post_likes_user ON post_likes (user_id);

CREATE TABLE post_comments
(
    id                BIGSERIAL  PRIMARY KEY,
    post_id           BIGINT     NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    parent_comment_id BIGINT     REFERENCES post_comments (id) ON DELETE CASCADE,
    author_id         BIGINT     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content           TEXT       NOT NULL,
    created_at        TIMESTAMP  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_post_comments_post_parent ON post_comments (post_id, parent_comment_id, created_at);
