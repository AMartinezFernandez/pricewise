CREATE TABLE company_api_keys (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    encrypted_key VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_company_provider UNIQUE (company_id, provider)
);

CREATE INDEX idx_company_api_keys_company ON company_api_keys(company_id);
