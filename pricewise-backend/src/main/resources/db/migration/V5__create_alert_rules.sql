-- Tabla de reglas de alerta configurables por empresa
CREATE TABLE alert_rules (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    product_id  BIGINT REFERENCES products(id) ON DELETE CASCADE,
    alert_type  VARCHAR(30) NOT NULL,
    name        VARCHAR(100),
    threshold   DECIMAL(5,2) NOT NULL DEFAULT 15.00,
    enabled     BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alert_rule_company ON alert_rules(company_id);
CREATE INDEX idx_alert_rule_type ON alert_rules(alert_type);
