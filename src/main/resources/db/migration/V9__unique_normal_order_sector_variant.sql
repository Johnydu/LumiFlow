CREATE UNIQUE INDEX IF NOT EXISTS uq_ordem_setor_linha_normal
    ON ordem_setor (ordem_producao_id, sequencia, (COALESCE(variante, '')))
    WHERE origem_refugo = FALSE;
