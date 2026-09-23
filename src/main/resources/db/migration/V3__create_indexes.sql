-- ============================================================================
-- 1. ÍNDICES ESTRATÉGICOS PARA DESEMPENHO E ESCALABILIDADE
-- ============================================================================

-- USUÁRIO E SEGURANÇA
CREATE INDEX IF NOT EXISTS idx_usuario_nivel_acesso ON usuario (nivel_acesso_id);
CREATE INDEX IF NOT EXISTS idx_usuario_setor ON usuario (setor_id);

-- OPERADOR
CREATE INDEX IF NOT EXISTS idx_operador_nome ON operador (nome);
CREATE INDEX IF NOT EXISTS idx_operador_setor_padrao ON operador (setor_padrao_id);
CREATE INDEX IF NOT EXISTS idx_setor_operador_operador ON setor_operador (operador_id);

-- MÁQUINA E ETAPAS
CREATE INDEX IF NOT EXISTS idx_maquina_setor ON maquina (setor_id);
CREATE INDEX IF NOT EXISTS idx_etapa_setor_setor ON etapa_setor (setor_id);

-- ROTEIRO
CREATE INDEX IF NOT EXISTS idx_roteiro_produto_id ON roteiro_produto (produto_id);
CREATE INDEX IF NOT EXISTS idx_roteiro_setor ON roteiro_produto (setor_id);

-- ORDEM DE PRODUÇÃO
CREATE INDEX IF NOT EXISTS idx_ordem_produto ON ordem_producao (produto_id);
CREATE INDEX IF NOT EXISTS idx_ordem_status ON ordem_producao (status);
CREATE INDEX IF NOT EXISTS idx_ordem_criado_por ON ordem_producao (criado_por_id);
CREATE INDEX IF NOT EXISTS idx_ordem_data_criacao ON ordem_producao (data_criacao DESC);

-- ORDEM SETOR (Consolidado com as melhorias de atualizações posteriores)
CREATE INDEX IF NOT EXISTS idx_ordem_setor_ordem_producao ON ordem_setor (ordem_producao_id);
CREATE INDEX IF NOT EXISTS idx_ordem_setor_setor ON ordem_setor (setor_id);
CREATE INDEX IF NOT EXISTS idx_ordem_setor_status ON ordem_setor (status);

-- LANÇAMENTO (Ajustado de usuario_id para operador_id)
CREATE INDEX IF NOT EXISTS idx_lancamento_ordem_setor ON lancamento (ordem_setor_id);
CREATE INDEX IF NOT EXISTS idx_lancamento_maquina ON lancamento (maquina_id);
CREATE INDEX IF NOT EXISTS idx_lancamento_operador ON lancamento (operador_id);
CREATE INDEX IF NOT EXISTS idx_lancamento_data_hora ON lancamento (data_hora DESC);

-- REFUGO E RETRABALHO
CREATE INDEX IF NOT EXISTS idx_refugo_ordem_setor ON refugo (ordem_setor_id);
CREATE INDEX IF NOT EXISTS idx_refugo_data_hora ON refugo (data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_retrabalho_refugo ON retrabalho (refugo_id);

-- VIDRO
CREATE INDEX IF NOT EXISTS idx_movimentacao_vidro_chapa ON movimentacao_vidro (chapa_vidro_id);
CREATE INDEX IF NOT EXISTS idx_movimentacao_vidro_data ON movimentacao_vidro (data_hora DESC);
CREATE INDEX IF NOT EXISTS idx_movimentacao_vidro_operador ON movimentacao_vidro (operador_id);