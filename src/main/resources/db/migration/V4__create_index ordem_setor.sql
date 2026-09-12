CREATE INDEX IF NOT EXISTS idx_ordem_setor_setor_id ON ordem_setor(setor_id);
CREATE INDEX IF NOT EXISTS idx_ordem_setor_status ON ordem_setor(status);
CREATE INDEX IF NOT EXISTS idx_ordem_setor_ordem_producao_id ON ordem_setor(ordem_producao_id);