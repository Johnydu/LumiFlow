-- ============================================================================
-- V4 — ROTEIRO GRAVADO NA ORDEM DE PRODUÇÃO
-- ============================================================================
-- A ordem passa a guardar o roteiro com que foi criada. Assim, trocar o roteiro
-- de um produto (ou criar uma nova versão por duplicação) não reinterpreta as
-- ordens abertas nem o histórico das concluídas.
-- ============================================================================

ALTER TABLE ordem_producao
    ADD COLUMN roteiro_id BIGINT NULL;

-- Ordens já existentes recebem o roteiro atual do produto.
UPDATE ordem_producao o
SET roteiro_id = p.roteiro_id
FROM produto p
WHERE p.id = o.produto_id;

ALTER TABLE ordem_producao
    ADD CONSTRAINT fk_ordem_roteiro
        FOREIGN KEY (roteiro_id) REFERENCES roteiro (id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_ordem_roteiro ON ordem_producao (roteiro_id);

-- NOT NULL somente quando o preenchimento acima cobriu todas as linhas.
-- (Ordem antiga cujo produto já estava sem roteiro deve ser tratada manualmente.)
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM ordem_producao WHERE roteiro_id IS NULL) THEN
            ALTER TABLE ordem_producao
                ALTER COLUMN roteiro_id SET NOT NULL;
        END IF;
    END
$$;

COMMENT ON COLUMN ordem_producao.roteiro_id IS 'Roteiro usado na criação da ordem; define as etapas e a sequência dela';