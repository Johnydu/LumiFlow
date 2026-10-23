-- ============================================================================
-- V5 — ÍNDICES PARA ESCALA
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_ordem_setor_setor_status_criado
    ON ordem_setor (setor_id, status, criado_em DESC);

DROP INDEX IF EXISTS idx_ordem_setor_setor;

DO
$$
    BEGIN
        CREATE EXTENSION IF NOT EXISTS pg_trgm;
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_produto_nome_trgm
                 ON produto USING gin (lower(nome) gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_produto_codigo_trgm
                 ON produto USING gin (lower(codigo) gin_trgm_ops)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_ordem_numero_trgm
                 ON ordem_producao USING gin (lower(numero) gin_trgm_ops)';
    EXCEPTION
        WHEN OTHERS THEN
            RAISE NOTICE 'pg_trgm indisponível; índices de busca por trecho não criados: %', SQLERRM;
    END
$$;
