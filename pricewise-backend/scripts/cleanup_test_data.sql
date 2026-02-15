-- ============================================================
-- Script de limpieza de datos de prueba
-- Conserva: usuario admin (id que indiques) y usuario carlos
-- Elimina: todos los demas usuarios, productos, y datos asociados
-- ============================================================
-- INSTRUCCIONES:
-- 1. Revisa los IDs de los usuarios que quieres conservar
-- 2. Ejecuta: psql -U <usuario> -d pricewise_db -f cleanup_test_data.sql
-- ============================================================

BEGIN;

-- Paso 1: Identificar usuarios a conservar
-- Ajusta estos WHERE segun tus usuarios reales
-- Puedes verificar primero con: SELECT id, username, email, role FROM users;

-- Paso 2: Eliminar datos dependientes de productos que se van a borrar
-- (productos de usuarios que NO son admin ni carlos)

-- Eliminar alertas de productos de empresas con datos de test
DELETE FROM alerts
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.company_id NOT IN (
        SELECT DISTINCT u.company_id FROM users u
        WHERE u.username IN ('admin', 'carlos')
        AND u.company_id IS NOT NULL
    )
);

-- Eliminar recomendaciones de precios de productos de test
DELETE FROM price_recommendations
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.company_id NOT IN (
        SELECT DISTINCT u.company_id FROM users u
        WHERE u.username IN ('admin', 'carlos')
        AND u.company_id IS NOT NULL
    )
);

-- Eliminar historial de precios de productos de test
DELETE FROM price_history
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.company_id NOT IN (
        SELECT DISTINCT u.company_id FROM users u
        WHERE u.username IN ('admin', 'carlos')
        AND u.company_id IS NOT NULL
    )
);

-- Eliminar precios de competidores de productos de test
DELETE FROM competitor_prices
WHERE product_id IN (
    SELECT p.id FROM products p
    WHERE p.company_id NOT IN (
        SELECT DISTINCT u.company_id FROM users u
        WHERE u.username IN ('admin', 'carlos')
        AND u.company_id IS NOT NULL
    )
);

-- Eliminar productos de empresas de test
DELETE FROM products
WHERE company_id NOT IN (
    SELECT DISTINCT u.company_id FROM users u
    WHERE u.username IN ('admin', 'carlos')
    AND u.company_id IS NOT NULL
);

-- Eliminar usuarios de test (conservar admin y carlos)
DELETE FROM users
WHERE username NOT IN ('admin', 'carlos');

-- Eliminar empresas sin usuarios
DELETE FROM companies
WHERE id NOT IN (
    SELECT DISTINCT company_id FROM users WHERE company_id IS NOT NULL
);

-- Eliminar competidores huerfanos
DELETE FROM competitors
WHERE company_id NOT IN (
    SELECT DISTINCT company_id FROM users WHERE company_id IS NOT NULL
);

-- Verificar resultado
SELECT 'Usuarios restantes:' AS info, count(*) AS total FROM users;
SELECT 'Productos restantes:' AS info, count(*) AS total FROM products;
SELECT 'Empresas restantes:' AS info, count(*) AS total FROM companies;

COMMIT;
