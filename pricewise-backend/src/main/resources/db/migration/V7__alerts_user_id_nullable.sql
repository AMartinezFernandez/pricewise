-- Permitir user_id NULL en alerts para conservar alertas al borrar usuarios.
-- Cambiar ON DELETE CASCADE a ON DELETE SET NULL.
ALTER TABLE alerts ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE alerts DROP CONSTRAINT IF EXISTS alerts_user_id_fkey;
ALTER TABLE alerts ADD CONSTRAINT alerts_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;
