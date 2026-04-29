# Migraciones de base de datos con Flyway

Flyway se ejecuta automáticamente al iniciar la aplicación.

## Perfiles

- `dev` y `prod`: Flyway habilitado, `ddl-auto: validate` (Hibernate solo valida, no modifica el esquema).
- `test`: Flyway deshabilitado, `ddl-auto: create-drop` con H2 en memoria.

## Base de datos existente

Con `baseline-on-migrate: true` y `baseline-version: 0`, en la primera ejecución Flyway:

1. Detecta que las tablas ya existen.
2. Crea la tabla `flyway_schema_history`.
3. Marca V1 como baseline sin ejecutarla.
4. Ejecuta a partir de V2 con normalidad.

En una base de datos vacía, Flyway ejecuta `V1__baseline.sql` y crea todo el esquema.

## Añadir una migración

1. Crear el archivo en `src/main/resources/db/migration/`.
2. Nombrar como `V{número}__{descripción}.sql`, por ejemplo `V2__add_composite_indexes.sql`.
3. Escribir SQL de PostgreSQL.
4. Usar `IF NOT EXISTS` o `IF EXISTS` cuando aplique.
5. Nunca modificar una migración ya ejecutada.

## Rollback

Flyway Community no soporta rollback automático. Para revertir:

1. Crear una migración nueva que deshaga los cambios:

   ```sql
   DROP INDEX IF EXISTS idx_new_index;
   ALTER TABLE products DROP COLUMN IF EXISTS new_column;
   ```

2. Ante un esquema corrupto, restaurar el último backup. Si no hay backup, ejecutar las sentencias inversas a mano y crear de inmediato una migración `Vn` que las recoja. No editar `flyway_schema_history` manualmente: rompe la integridad del historial.

## Migraciones existentes

1. **V1** (2026-02-22): esquema inicial baseline. Todas las tablas, constraints e índices.
2. **V2** (2026-02-22): índices compuestos para queries frecuentes.
3. **V3** (2026-02-22): tabla `audit_logs`. No utilizada por el código actual.
4. **V4** (2026-02-22): campo `auth_provider` en `users` para Google OAuth.
5. **V5** (2026-02-22): tabla `alert_rules` con `company_id`, `product_id`, `alert_type`, `threshold`, `enabled`.
6. **V6** (2026-03-08): campo `target_price` (`DECIMAL(10,2)`) en `alert_rules`.
7. **V7** (2026-03-08): `user_id` nullable en `alerts`, `ON DELETE SET NULL` para conservar alertas al borrar usuarios.
8. **V8** (2026-03-09): tabla `company_api_keys`. Claves cifradas AES-256 por empresa, índice único por `company + provider`.

## Documentación relacionada

- `ARQUITECTURA.md`, sección 6 sobre entidades JPA y persistencia.
- `SEGURIDAD.md`, cifrado de BD, constraints y transacciones.
