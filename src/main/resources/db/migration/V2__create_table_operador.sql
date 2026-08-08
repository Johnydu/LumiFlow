-- =============================================================================
-- LUMIFLOW - Migration: Tabela de Operadores e Vínculo com Setores (PostgreSQL)
-- =============================================================================

-- 1. Criação da Tabela de Operadores
CREATE TABLE operador (
                          id BIGSERIAL PRIMARY KEY,
                          nome VARCHAR(100) NOT NULL,
                          funcao VARCHAR(30),
                          setor_padrao_id BIGINT,

    -- Campos de Auditoria
                          criado_em TIMESTAMP WITHOUT TIME ZONE,
                          criado_por VARCHAR(100),
                          atualizado_em TIMESTAMP WITHOUT TIME ZONE,
                          atualizado_por VARCHAR(100),

    -- FK para o Setor Padrão do Operador
                          CONSTRAINT fk_operador_setor_padrao
                              FOREIGN KEY (setor_padrao_id)
                                  REFERENCES setor(id)
                                  ON DELETE SET NULL
);

-- 2. Tabela de Junção (Setor <-> Operador para a relação @ManyToMany)
CREATE TABLE setor_operador (
                                setor_id BIGINT NOT NULL,
                                operador_id BIGINT NOT NULL,

                                CONSTRAINT pk_setor_operador PRIMARY KEY (setor_id, operador_id),

                                CONSTRAINT fk_setor_operador_setor
                                    FOREIGN KEY (setor_id)
                                        REFERENCES setor(id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_setor_operador_operador
                                    FOREIGN KEY (operador_id)
                                        REFERENCES operador(id)
                                        ON DELETE CASCADE
);

-- 3. Índices de Performance
CREATE INDEX idx_operador_nome ON operador(nome);
CREATE INDEX idx_operador_setor_padrao ON operador(setor_padrao_id);
CREATE INDEX idx_setor_operador_operador ON setor_operador(operador_id);