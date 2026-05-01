ALTER TABLE recipes
    ADD CONSTRAINT fk_recipes_author_id
        FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE;
