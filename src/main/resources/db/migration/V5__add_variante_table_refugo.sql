-- V4__add_variante_refugo.sql
ALTER TABLE refugo ADD COLUMN variante VARCHAR(50) NULL;

-- Índice composto para a busca de OrdemSetor por refugo/retrabalho
-- (ordemId + setorId + sequencia + status), usada a cada lançamento de refugo/retrabalho
CREATE INDEX IF NOT EXISTS idx_ordem_setor_refugo_lookup
    ON ordem_setor (ordem_producao_id, setor_id, sequencia, status);