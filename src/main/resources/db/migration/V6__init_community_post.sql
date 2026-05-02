CREATE TABLE posts
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title         VARCHAR(100) NOT NULL,
    content       TEXT         NOT NULL,
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_user ON posts (user_id);
CREATE INDEX idx_posts_created_at ON posts (created_at DESC);

CREATE TABLE post_images
(
    id            BIGSERIAL PRIMARY KEY,
    post_id       BIGINT       NOT NULL REFERENCES posts (id) ON DELETE CASCADE,
    image_url     VARCHAR(512) NOT NULL,
    display_order SMALLINT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_post_images_post ON post_images (post_id);
