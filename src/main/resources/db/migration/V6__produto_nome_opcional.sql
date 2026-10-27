-- ============================================================================
-- V6 — NOME DO PRODUTO OPCIONAL
-- ============================================================================
-- O código é a identificação principal do produto; o nome passa a ser opcional.
-- ============================================================================

ALTER TABLE produto
    ALTER COLUMN nome DROP NOT NULL;

COMMENT ON COLUMN produto.nome IS 'Nome opcional; o código é a identificação principal do produto';