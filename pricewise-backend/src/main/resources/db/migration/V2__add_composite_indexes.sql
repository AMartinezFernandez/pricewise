-- V2__add_composite_indexes.sql
-- Indices compuestos para mejorar rendimiento en queries frecuentes

CREATE INDEX IF NOT EXISTS idx_competitor_price_product_scraped
    ON competitor_prices (product_id, scraped_at DESC);

CREATE INDEX IF NOT EXISTS idx_price_history_product_recorded
    ON price_history (product_id, recorded_at DESC);
