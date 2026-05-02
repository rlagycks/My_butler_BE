CREATE TABLE post_likes (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_post_likes UNIQUE (post_id, user_id)
);
CREATE INDEX idx_post_likes_post ON post_likes (post_id);

CREATE TABLE comments (
    id                BIGSERIAL PRIMARY KEY,
    post_id           BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id           BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_comment_id BIGINT REFERENCES comments(id) ON DELETE CASCADE,
    content           TEXT NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_comments_post ON comments (post_id);
CREATE INDEX idx_comments_parent ON comments (parent_comment_id);
