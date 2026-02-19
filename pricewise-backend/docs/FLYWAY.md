# Migraciones de Base de Datos con Flyway

## Configuracion

Flyway esta integrado en el proyecto y se ejecuta automaticamente al iniciar la aplicacion.

### Perfiles
- **dev/prod**: Flyway habilitado, `ddl-auto: validate` (Hibernate solo valida, no modifica schema)
- **test**: Flyway deshabilitado, `ddl-auto: create-drop` (H2 en memoria)

### Base de datos existente
La configuracion `baseline-on-migrate: true` con `baseline-version: 0` permite que Flyway se integre en bases de datos que ya tienen tablas. Al ejecutar por primera vez, Flyway:
1. Detecta que las tablas ya existen
2. Crea la tabla `flyway_schema_history`
3. Marca V1 como baseline (no la ejecuta)
4. Futuras migraciones (V2, V3...) se ejecutan normalmente

### Base de datos nueva
En una base de datos vacia, Flyway ejecuta V1__baseline.sql creando todo el schema desde cero.

## Como añadir una nueva migracion

1. Crear archivo en `src/main/resources/db/migration/`
2. Nombrar siguiendo la convencion: `V{numero}__{descripcion}.sql`
   - Ejemplo: `V2__add_composite_indexes.sql`
3. Escribir SQL puro de PostgreSQL
4. Usar `IF NOT EXISTS` / `IF EXISTS` cuando sea posible
5. **Nunca modificar** una migracion ya ejecutada

## Rollback

Flyway Community Edition no soporta rollback automatico. Para revertir:

1. Crear una nueva migracion que deshaga los cambios:
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

| Version | Descripcion | Fecha |
|---------|-------------|-------|
| V1 | Schema inicial (baseline) | 2026-02-22 |
