-- V4__add_auth_provider_to_users.sql
-- Añade columna auth_provider a users para soportar Google Sign-In.
-- Valor por defecto 'LOCAL' para usuarios existentes (registro con email/password).

ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(10) NOT NULL DEFAULT 'LOCAL';
