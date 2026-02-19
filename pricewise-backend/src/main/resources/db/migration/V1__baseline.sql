-- V1__baseline.sql
-- Esquema inicial de la base de datos PriceWise
-- NOTA: En bases de datos existentes, Flyway marcara esta migracion como baseline sin ejecutarla.

-- ============================================================================
-- 1. COMPANIES
-- ============================================================================
CREATE TABLE IF NOT EXISTS companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    company_code VARCHAR(8) NOT NULL UNIQUE,
    business_type VARCHAR(50),
    tax_id VARCHAR(20) UNIQUE,
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    shared_stock_enabled BOOLEAN NOT NULL DEFAULT true,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============================================================================
-- 2. USERS
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password TEXT NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    active BOOLEAN NOT NULL DEFAULT true,
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_company ON users(company_id);

-- ============================================================================
-- 3. PRODUCTS
-- ============================================================================
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    sku VARCHAR(50),
    ean VARCHAR(50),
    asin VARCHAR(20),
    current_price DECIMAL(12, 2) NOT NULL,
    cost_price DECIMAL(12, 2),
    min_margin DECIMAL(5, 2) DEFAULT 0.10,
    category VARCHAR(100),
    brand VARCHAR(100),
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT true,
    monitoring_enabled BOOLEAN NOT NULL DEFAULT true,
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_product_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_product_company ON products(company_id);
CREATE INDEX IF NOT EXISTS idx_product_category ON products(category);

-- Partial unique index: unicidad de SKU por empresa solo entre productos activos
CREATE UNIQUE INDEX IF NOT EXISTS uix_product_sku_company_active
    ON products(sku, company_id) WHERE active = true;

-- ============================================================================
-- 4. PRICE_HISTORY
-- ============================================================================
CREATE TABLE IF NOT EXISTS price_history (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    price DECIMAL(12, 2) NOT NULL,
    previous_price DECIMAL(12, 2),
    change_type VARCHAR(20),
    change_reason VARCHAR(200),
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_price_history_product ON price_history(product_id);
CREATE INDEX IF NOT EXISTS idx_price_history_date ON price_history(recorded_at);

-- ============================================================================
-- 5. COMPETITORS
-- ============================================================================
CREATE TABLE IF NOT EXISTS competitors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    code VARCHAR(255) NOT NULL,
    base_url VARCHAR(255) NOT NULL,
    logo_url VARCHAR(255),
    source_type VARCHAR(255) NOT NULL,
    source_config TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    last_scraped_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============================================================================
-- 6. COMPETITOR_PRICES
-- ============================================================================
CREATE TABLE IF NOT EXISTS competitor_prices (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    competitor_id BIGINT NOT NULL REFERENCES competitors(id) ON DELETE CASCADE,
    product_url VARCHAR(1000),
    competitor_product_title TEXT,
    price DECIMAL(10, 2) NOT NULL,
    original_price DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'EUR',
    available BOOLEAN NOT NULL DEFAULT true,
    free_shipping BOOLEAN NOT NULL DEFAULT false,
    shipping_cost DECIMAL(10, 2),
    scraped_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_competitor_price_product ON competitor_prices(product_id);
CREATE INDEX IF NOT EXISTS idx_competitor_price_competitor ON competitor_prices(competitor_id);
CREATE INDEX IF NOT EXISTS idx_competitor_price_scraped_at ON competitor_prices(scraped_at);

-- ============================================================================
-- 7. ALERTS
-- ============================================================================
CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    alert_type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(500),
    previous_price DECIMAL(12, 2),
    new_price DECIMAL(12, 2),
    change_percent DECIMAL(5, 2),
    is_read BOOLEAN NOT NULL DEFAULT false,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_alert_user ON alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_alert_product ON alerts(product_id);
CREATE INDEX IF NOT EXISTS idx_alert_read ON alerts(is_read);
CREATE INDEX IF NOT EXISTS idx_alert_type ON alerts(alert_type);

-- ============================================================================
-- 8. PRICE_RECOMMENDATIONS
-- ============================================================================
CREATE TABLE IF NOT EXISTS price_recommendations (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    recommendation_type VARCHAR(30) NOT NULL,
    current_price DECIMAL(12, 2) NOT NULL,
    competitor_price DECIMAL(12, 2) NOT NULL,
    suggested_price DECIMAL(12, 2) NOT NULL,
    price_difference_percent DECIMAL(5, 2),
    potential_saving_or_profit DECIMAL(12, 2),
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_at TIMESTAMP,
    dismissed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recommendation_product ON price_recommendations(product_id);
CREATE INDEX IF NOT EXISTS idx_recommendation_status ON price_recommendations(status);
CREATE INDEX IF NOT EXISTS idx_recommendation_type ON price_recommendations(recommendation_type);
