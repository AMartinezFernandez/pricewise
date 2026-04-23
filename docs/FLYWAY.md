# Migraciones de base de datos con Flyway

## Configuración

Flyway está integrado en el proyecto y se ejecuta automáticamente al iniciar la aplicación.

### Perfiles

- **dev / prod**: Flyway habilitado, `ddl-auto: validate` (Hibernate solo valida, no modifica el esquema).
- **test**: Flyway deshabilitado, `ddl-auto: create-drop` (H2 en memoria).

### Base de datos existente

La configuración `baseline-on-migrate: true` con `baseline-version: 0` permite que Flyway se integre en bases de datos que ya tienen tablas. Al ejecutarse por primera vez, Flyway:

1. Detecta que las tablas ya existen.
2. Crea la tabla `flyway_schema_history`.
3. Marca V1 como baseline (no la ejecuta).
4. Ejecuta normalmente las migraciones posteriores (V2, V3…).

### Base de datos nueva

En una base de datos vacía, Flyway ejecuta `V1__baseline.sql` creando todo el esquema desde cero.

## Cómo añadir una nueva migración

1. Crear archivo en `src/main/resources/db/migration/`.
2. Nombrar siguiendo la convención: `V{número}__{descripción}.sql`.
   - Ejemplo: `V2__add_composite_indexes.sql`.
3. Escribir SQL puro de PostgreSQL.
4. Usar `IF NOT EXISTS` / `IF EXISTS` cuando sea posible.
5. **Nunca modificar** una migración ya ejecutada.

## Rollback

Flyway Community Edition no soporta rollback automático. Para revertir:

1. Crear una nueva migración que deshaga los cambios:

   ```sql
   -- V3__revert_v2_changes.sql
   DROP INDEX IF EXISTS idx_new_index;
   ALTER TABLE products DROP COLUMN IF EXISTS new_column;
   ```

2. En caso de emergencia, revertir manualmente en la BD y limpiar el historial:

   ```sql
   DELETE FROM flyway_schema_history WHERE version = '2';
   ```

## Migraciones existentes

| Versión | Descripción | Fecha |
|---------|-------------|-------|
| V1 | Esquema inicial (baseline) — todas las tablas, constraints e índices | 2026-02-22 |
| V2 | Índices compuestos para rendimiento de queries frecuentes | 2026-02-22 |
| V3 | Tabla `audit_logs` (retirada del MVP, la migración se mantiene) | 2026-02-22 |
| V4 | Campo `auth_provider` en tabla `users` (soporte Google OAuth) | 2026-02-22 |
| V5 | Tabla `alert_rules` con `company_id`, `product_id`, `alert_type`, `threshold`, `enabled` | 2026-02-22 |
| V6 | Campo `target_price` (`DECIMAL(10,2)`) en `alert_rules` | 2026-03-08 |
| V7 | `user_id` nullable en `alerts` (`ON DELETE SET NULL` para conservar alertas al borrar usuarios) | 2026-03-08 |
| V8 | Tabla `company_api_keys` (API keys cifradas AES-256 por empresa, índice único por `company + provider`) | 2026-03-09 |

## Documentación relacionada

- [`ARQUITECTURA.md`](ARQUITECTURA.md) — Guía de arquitectura (sección 6: Entidades JPA y persistencia).
- [`SEGURIDAD.md`](SEGURIDAD.md) — Seguridad de BD (cifrado, constraints, transacciones).
- [`CRONOGRAMA.md`](CRONOGRAMA.md) — Fase 17 (integración de Flyway) y fases posteriores.
