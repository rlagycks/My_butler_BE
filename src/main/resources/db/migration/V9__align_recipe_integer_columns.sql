ALTER TABLE recipes
    ALTER COLUMN difficulty TYPE INTEGER;

ALTER TABLE recipe_ingredients
    ALTER COLUMN display_order TYPE INTEGER;

ALTER TABLE recipe_steps
    ALTER COLUMN step_order TYPE INTEGER;

ALTER TABLE recipe_ratings
    ALTER COLUMN score TYPE INTEGER;
