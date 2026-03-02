-- Añadir campo target_price a alert_rules para alertas por precio exacto
ALTER TABLE alert_rules ADD COLUMN target_price DECIMAL(10,2);
