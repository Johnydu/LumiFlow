-- ============================================================================
-- 1. FUNÇÕES UTILITÁRIAS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_atualizado_em_column()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_atualizado_em_column() IS 'Atualiza atualizado_em antes de cada UPDATE em tabelas auditadas';

-- ============================================================================
-- 2. TRIGGERS PARA AUDITORIA (atualizado_em)
-- ============================================================================

CREATE TRIGGER trg_nivel_acesso_atualizado_em
    BEFORE UPDATE ON nivel_acesso FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_setor_atualizado_em
    BEFORE UPDATE ON setor FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_usuario_atualizado_em
    BEFORE UPDATE ON usuario FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_operador_atualizado_em
    BEFORE UPDATE ON operador FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_maquina_atualizado_em
    BEFORE UPDATE ON maquina FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_etapa_setor_atualizado_em
    BEFORE UPDATE ON etapa_setor FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_produto_atualizado_em
    BEFORE UPDATE ON produto FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_roteiro_produto_atualizado_em
    BEFORE UPDATE ON roteiro_produto FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_ordem_producao_atualizado_em
    BEFORE UPDATE ON ordem_producao FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_ordem_setor_atualizado_em
    BEFORE UPDATE ON ordem_setor FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_refugo_atualizado_em
    BEFORE UPDATE ON refugo FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_retrabalho_atualizado_em
    BEFORE UPDATE ON retrabalho FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_chapa_vidro_atualizado_em
    BEFORE UPDATE ON chapa_vidro FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();

CREATE TRIGGER trg_movimentacao_vidro_atualizado_em
    BEFORE UPDATE ON movimentacao_vidro FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em_column();