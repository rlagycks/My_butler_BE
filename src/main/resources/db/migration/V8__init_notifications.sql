CREATE TABLE notifications
(
    id                BIGSERIAL    PRIMARY KEY,
    recipient_user_id BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    actor_user_id     BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    type              VARCHAR(30)  NOT NULL,
    target_id         BIGINT,
    message           TEXT         NOT NULL,
    is_read           BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at           TIMESTAMP,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_user_id, is_read, created_at DESC);
CREATE INDEX idx_notifications_type_target ON notifications (type, target_id, created_at DESC);
