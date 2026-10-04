ALTER TABLE refugo
    ADD COLUMN ordem_setor_retrabalho_id BIGINT NULL,
    ADD CONSTRAINT fk_refugo_ordem_setor_retrabalho
        FOREIGN KEY (ordem_setor_retrabalho_id) REFERENCES ordem_setor (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_refugo_ordem_setor_retrabalho
    ON refugo (ordem_setor_retrabalho_id);