-- ============================================================================
-- V3 — ÍNDICES DE DESEMPENHO
-- ============================================================================
-- Regras:
--   * PostgreSQL NÃO cria índice automático para colunas de chave estrangeira:
--     todas as FKs têm índice (acelera JOINs e DELETEs na tabela referenciada).
--   * Constraints UNIQUE (V1) já criam seus próprios índices; não são repetidos aqui.
--   * IF NOT EXISTS torna a migration segura para reexecução.
-- ============================================================================

-- USUÁRIO E OPERADOR
CREATE INDEX IF NOT EXISTS idx_usuario_nivel_acesso ON usuario (nivel_acesso_id);
CREATE INDEX IF NOT EXISTS idx_usuario_setor ON usuario (setor_id);
CREATE INDEX IF NOT EXISTS idx_operador_setor_padrao ON operador (setor_padrao_id);

-- MÁQUINA
CREATE INDEX IF NOT EXISTS idx_maquina_setor ON maquina (setor_id);

-- ROTEIRO (roteiro_id é coberto por uq_passo_sequencia; etapa_setor.setor_id por uq_etapa_setor_ordem)
CREATE INDEX IF NOT EXISTS idx_produto_roteiro ON produto (roteiro_id);
CREATE INDEX IF NOT EXISTS idx_roteiro_passo_setor ON roteiro_passo (setor_id);
CREATE INDEX IF NOT EXISTS idx_roteiro_passo_etapa ON roteiro_passo (etapa_setor_id);

-- ORDEM DE PRODUÇÃO
-- (produto_id, status) atende a FK e a consulta "existe ordem aberta deste produto".
CREATE INDEX IF NOT EXISTS idx_ordem_produto_status ON ordem_producao (produto_id, status);
CREATE INDEX IF NOT EXISTS idx_ordem_status ON ordem_producao (status);
CREATE INDEX IF NOT EXISTS idx_ordem_criado_por ON ordem_producao (criado_por_id);
CREATE INDEX IF NOT EXISTS idx_ordem_data_criacao ON ordem_producao (data_criacao DESC);

-- ORDEM SETOR
CREATE INDEX IF NOT EXISTS idx_ordem_setor_ordem_producao ON ordem_setor (ordem_producao_id);
CREATE INDEX IF NOT EXISTS idx_ordem_setor_setor ON ordem_setor (setor_id);
CREATE INDEX IF NOT EXISTS idx_ordem_setor_status ON ordem_setor (status);

-- Regra de negócio: no máximo UMA linha normal por ordem + sequência + variante.
-- Linhas criadas por refugo (origem_refugo = TRUE) ficam fora da regra.
CREATE UNIQUE INDEX IF NOT EXISTS uq_ordem_setor_linha_normal
    ON ordem_setor (ordem_producao_id, sequencia, (COALESCE(variante, '')))
    WHERE origem_refugo = FALSE;

-- LANÇAMENTO
CREATE INDEX IF NOT EXISTS idx_lancamento_ordem_setor ON lancamento (ordem_setor_id);
CREATE INDEX IF NOT EXISTS idx_lancamento_maquina ON lancamento (maquina_id);
CREATE INDEX IF NOT EXISTS idx_lancamento_operador ON lancamento (operador_id);
CREATE INDEX IF NOT EXISTS idx_lancamento_etapa ON lancamento (etapa_setor_id);
CREATE INDEX IF NOT EXISTS idx_lancamento_data_hora ON lancamento (data_hora DESC);

-- REFUGO E RETRABALHO
CREATE INDEX IF NOT EXISTS idx_refugo_ordem_setor ON refugo (ordem_setor_id);
CREATE INDEX IF NOT EXISTS idx_refugo_setor_origem ON refugo (setor_origem_id);
CREATE INDEX IF NOT EXISTS idx_refugo_operador ON refugo (operador_id);
CREATE INDEX IF NOT EXISTS idx_refugo_ordem_setor_retrabalho ON refugo (ordem_setor_retrabalho_id);
CREATE INDEX IF NOT EXISTS idx_refugo_data_hora ON refugo (data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_retrabalho_refugo ON retrabalho (refugo_id);
CREATE INDEX IF NOT EXISTS idx_retrabalho_maquina ON retrabalho (maquina_id);
CREATE INDEX IF NOT EXISTS idx_retrabalho_operador ON retrabalho (operador_id);

-- VIDRO
-- (chapa, tipo) atende a FK e a soma de saldo por chapa e tipo de movimentação.
CREATE INDEX IF NOT EXISTS idx_movimentacao_vidro_chapa_tipo ON movimentacao_vidro (chapa_vidro_id, tipo_movimentacao);
CREATE INDEX IF NOT EXISTS idx_movimentacao_vidro_operador ON movimentacao_vidro (operador_id);
CREATE INDEX IF NOT EXISTS idx_movimentacao_vidro_data ON movimentacao_vidro (data_hora DESC);