CREATE TABLE inventory_items
(
    id             BIGSERIAL    PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    category       VARCHAR(50)  NOT NULL,
    abv            DECIMAL(4, 1),
    capacity_ml    INTEGER,
    level_status   VARCHAR(10)  NOT NULL,
    purchase_price INTEGER,
    is_opened      BOOLEAN      NOT NULL DEFAULT FALSE,
    opened_at      TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inventory_user_category ON inventory_items (user_id, category);
CREATE INDEX idx_inventory_user_level    ON inventory_items (user_id, level_status);
CREATE INDEX idx_inventory_user_opened   ON inventory_items (user_id, opened_at);
