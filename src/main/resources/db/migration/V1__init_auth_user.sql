-- Users table
CREATE TABLE users
(
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    username            VARCHAR(50)  NOT NULL UNIQUE,
    password            VARCHAR(255) NOT NULL,
    gender              VARCHAR(20),
    age_group           VARCHAR(20),
    drinking_frequency  VARCHAR(30),
    onboarding_completed BOOLEAN     NOT NULL DEFAULT FALSE,
    terms_agreed        BOOLEAN      NOT NULL DEFAULT FALSE,
    privacy_agreed      BOOLEAN      NOT NULL DEFAULT FALSE,
    marketing_agreed    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- User preferences table
CREATE TABLE user_preferences
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT      NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    preferred_abv    VARCHAR(20),
    experience_level VARCHAR(20),
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- User taste preferences (multi-select)
CREATE TABLE user_taste_preferences
(
    user_preference_id BIGINT      NOT NULL REFERENCES user_preferences (id) ON DELETE CASCADE,
    taste              VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_preference_id, taste)
);

-- Refresh tokens table
CREATE TABLE refresh_tokens
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
