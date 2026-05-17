ALTER TABLE inventory_items
    ADD COLUMN tasting_notes    TEXT,
    ADD COLUMN purchased_at     DATE,
    ADD COLUMN purchase_place   VARCHAR(200),
    ADD COLUMN origin           VARCHAR(100);
