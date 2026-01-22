-- =========================================
-- Índices para optimizar búsquedas - PriceWise
-- =========================================
-- Ejecutar en PostgreSQL después de crear las tablas

-- Índices para tabla USERS
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);

CREATE INDEX IF NOT EXISTS idx_users_active ON users (active);

-- Índices para tabla PRODUCTS
CREATE INDEX IF NOT EXISTS idx_products_user_id ON products (user_id);

CREATE INDEX IF NOT EXISTS idx_products_sku ON products (sku);

CREATE INDEX IF NOT EXISTS idx_products_ean ON products (ean);

CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);

CREATE INDEX IF NOT EXISTS idx_products_brand ON products (brand);

CREATE INDEX IF NOT EXISTS idx_products_name_gin ON products USING gin (to_tsvector('spanish', name));

-- Índices para tabla PRICE_HISTORY
CREATE INDEX IF NOT EXISTS idx_price_history_product_id ON price_history (product_id);

CREATE INDEX IF NOT EXISTS idx_price_history_recorded_at ON price_history (recorded_at DESC);

-- Índices para tabla COMPETITORS
CREATE INDEX IF NOT EXISTS idx_competitors_code ON competitors (code);

CREATE INDEX IF NOT EXISTS idx_competitors_active ON competitors (active);

-- Índices para tabla COMPETITOR_PRICES
CREATE INDEX IF NOT EXISTS idx_competitor_prices_product_id ON competitor_prices (product_id);

CREATE INDEX IF NOT EXISTS idx_competitor_prices_competitor_id ON competitor_prices (competitor_id);

CREATE INDEX IF NOT EXISTS idx_competitor_prices_scraped_at ON competitor_prices (scraped_at DESC);

-- Índice compuesto para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_competitor_prices_product_competitor ON competitor_prices (product_id, competitor_id);