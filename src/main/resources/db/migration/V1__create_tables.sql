-- ============================================================================
-- 1. SEGURANÇA E CONTROLE DE ACESSO
-- ============================================================================

CREATE TABLE nivel_acesso
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descricao     VARCHAR(255) NOT NULL UNIQUE,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NULL
);
COMMENT ON TABLE nivel_acesso IS 'Níveis de permissão: SUPORTE, GESTAO, PCP_SUPERVISOR, OPERADOR';

CREATE TABLE setor
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(50) NOT NULL UNIQUE,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL
);
COMMENT ON TABLE setor IS 'Setores de produção (ex: Corte, Dobra, Pintura)';

CREATE TABLE usuario
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome            VARCHAR(50)  NOT NULL,
    login           VARCHAR(20)  NOT NULL UNIQUE,
    senha           VARCHAR(255) NOT NULL,
    nivel_acesso_id BIGINT       NOT NULL,
    setor_id        BIGINT       NULL,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMPTZ  NULL,
    CONSTRAINT fk_usuario_nivel_acesso
        FOREIGN KEY (nivel_acesso_id) REFERENCES nivel_acesso (id),
    CONSTRAINT fk_usuario_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE SET NULL
);
COMMENT ON TABLE usuario IS 'Usuários do sistema com controle de acesso';

CREATE TABLE operador
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome            VARCHAR(100) NOT NULL,
    funcao          VARCHAR(30),
    setor_padrao_id BIGINT,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por      VARCHAR(100),
    atualizado_em   TIMESTAMPTZ  NULL,
    atualizado_por  VARCHAR(100),
    CONSTRAINT fk_operador_setor_padrao
        FOREIGN KEY (setor_padrao_id) REFERENCES setor (id) ON DELETE SET NULL
);
COMMENT ON TABLE operador IS 'Operadores de produção alocados nos setores';

CREATE TABLE setor_operador
(
    setor_id    BIGINT NOT NULL,
    operador_id BIGINT NOT NULL,
    CONSTRAINT pk_setor_operador PRIMARY KEY (setor_id, operador_id),
    CONSTRAINT fk_setor_operador_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE CASCADE,
    CONSTRAINT fk_setor_operador_operador
        FOREIGN KEY (operador_id) REFERENCES operador (id) ON DELETE CASCADE
);
COMMENT ON TABLE setor_operador IS 'Associação: Operadores podem trabalhar em múltiplos setores';

-- ============================================================================
-- 2. ESTRUTURA DE PRODUÇÃO
-- ============================================================================

CREATE TABLE maquina
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(50) NOT NULL,
    setor_id      BIGINT      NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL,
    CONSTRAINT fk_maquina_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE CASCADE
);
COMMENT ON TABLE maquina IS 'Máquinas instaladas em cada setor de produção';

CREATE TABLE etapa_setor
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(50) NOT NULL,
    ordem         INT         NOT NULL,
    setor_id      BIGINT      NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL,
    CONSTRAINT fk_etapa_setor_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE CASCADE,
    CONSTRAINT uq_etapa_setor_ordem
        UNIQUE (setor_id, ordem)
);
COMMENT ON TABLE etapa_setor IS 'Etapas sequenciais dentro de cada setor';

CREATE TABLE produto
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    codigo        VARCHAR(30)  NOT NULL UNIQUE,
    descricao     VARCHAR(255) NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NULL
);
COMMENT ON TABLE produto IS 'Catálogo de produtos fabricados';

CREATE TABLE roteiro_produto
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sequencia      INT    NOT NULL,
    produto_id     BIGINT NOT NULL,
    setor_id       BIGINT NOT NULL,
    etapa_setor_id BIGINT NULL,
    CONSTRAINT fk_roteiro_produto
        FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE CASCADE,
    CONSTRAINT fk_roteiro_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE CASCADE,
    CONSTRAINT fk_roteiro_etapa
        FOREIGN KEY (etapa_setor_id) REFERENCES etapa_setor (id) ON DELETE SET NULL,
    CONSTRAINT uq_roteiro_sequencia
        UNIQUE (produto_id, sequencia)
);
COMMENT ON TABLE roteiro_produto IS 'Define o percurso de cada produto pela produção';

-- ============================================================================
-- 3. ORDENS DE PRODUÇÃO
-- ============================================================================

CREATE SEQUENCE ordem_producao_numero_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE ordem_producao
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero        VARCHAR(255) NOT NULL UNIQUE,
    quantidade    INT          NOT NULL,
    status        VARCHAR(25)  NOT NULL,
    produto_id    BIGINT       NOT NULL,
    criado_por_id BIGINT       NOT NULL,
    observacao    VARCHAR(500) NULL,
    data_criacao  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NULL,
    CONSTRAINT fk_ordem_produto
        FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ordem_criado_por
        FOREIGN KEY (criado_por_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ordem_quantidade
        CHECK (quantidade > 0)
);
COMMENT ON TABLE ordem_producao IS 'Ordens de produção abertas pelo PCP';

CREATE TABLE ordem_setor
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sequencia         INT         NOT NULL,
    qtd_recebida      INT         NOT NULL DEFAULT 0,
    qtd_produzida     INT         NOT NULL DEFAULT 0,
    qtd_pendente      INT         NOT NULL DEFAULT 0,
    status            VARCHAR(25) NOT NULL,
    variante          VARCHAR(50) NULL,
    origem_refugo     BOOLEAN     NOT NULL DEFAULT FALSE,
    inicio            TIMESTAMPTZ NULL,
    fim               TIMESTAMPTZ NULL,
    ordem_producao_id BIGINT      NOT NULL,
    setor_id          BIGINT      NOT NULL,
    etapa_setor_id    BIGINT      NULL,
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMPTZ NULL,
    CONSTRAINT fk_ordem_setor_ordem
        FOREIGN KEY (ordem_producao_id) REFERENCES ordem_producao (id) ON DELETE CASCADE,
    CONSTRAINT fk_ordem_setor_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ordem_setor_etapa
        FOREIGN KEY (etapa_setor_id) REFERENCES etapa_setor (id) ON DELETE SET NULL,
    CONSTRAINT chk_ordem_setor_quantidades
        CHECK (qtd_recebida >= 0 AND qtd_produzida >= 0 AND qtd_pendente >= 0)
);
COMMENT ON TABLE ordem_setor IS 'Tracking do andamento da ordem em cada setor';

CREATE TABLE lancamento
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    qtd_produzida  INT          NOT NULL,
    observacao     VARCHAR(255) NULL,
    data_hora      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ordem_setor_id BIGINT       NOT NULL,
    maquina_id     BIGINT       NOT NULL,
    operador_id    BIGINT       NOT NULL,
    etapa_setor_id BIGINT       NULL,
    CONSTRAINT fk_lancamento_ordem_setor
        FOREIGN KEY (ordem_setor_id) REFERENCES ordem_setor (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lancamento_maquina
        FOREIGN KEY (maquina_id) REFERENCES maquina (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lancamento_operador
        FOREIGN KEY (operador_id) REFERENCES operador (id) ON DELETE RESTRICT,
    CONSTRAINT fk_lancamento_etapa
        FOREIGN KEY (etapa_setor_id) REFERENCES etapa_setor (id) ON DELETE SET NULL,
    CONSTRAINT chk_lancamento_quantidade
        CHECK (qtd_produzida > 0)
);
COMMENT ON TABLE lancamento IS 'Registro de produção realizada por operador em máquina';

-- ============================================================================
-- 4. QUALIDADE E CONTROLE
-- ============================================================================

CREATE TABLE refugo
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    qtd_refugo       INT          NOT NULL,
    motivo           VARCHAR(255) NOT NULL,
    destino          VARCHAR(30)  NOT NULL,
    data_hora        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ordem_setor_id   BIGINT       NOT NULL,
    setor_origem_id  BIGINT       NOT NULL,
    operador_id       BIGINT       NOT NULL,
    sequencia_origem INT          NULL,
    criado_em        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em    TIMESTAMPTZ  NULL,
    CONSTRAINT fk_refugo_ordem_setor
        FOREIGN KEY (ordem_setor_id) REFERENCES ordem_setor (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refugo_setor_origem
        FOREIGN KEY (setor_origem_id) REFERENCES setor (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refugo_usuario
        FOREIGN KEY (operador_id) REFERENCES operador (id) ON DELETE RESTRICT,
    CONSTRAINT chk_refugo_destino
        CHECK (destino IN ('RETRABALHO', 'DESCARTE')),
    CONSTRAINT chk_refugo_quantidade
        CHECK (qtd_refugo > 0)
);
COMMENT ON TABLE refugo IS 'Registro de peças refugadas com motivo e destino';

CREATE TABLE retrabalho
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    qtd_refeita   INT          NOT NULL,
    observacao    VARCHAR(255) NULL,
    data_hora     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refugo_id     BIGINT       NOT NULL,
    maquina_id    BIGINT       null ,
    operador_id    BIGINT       NOT NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NULL,
    CONSTRAINT fk_retrabalho_refugo
        FOREIGN KEY (refugo_id) REFERENCES refugo (id) ON DELETE CASCADE,
    CONSTRAINT fk_retrabalho_maquina
        FOREIGN KEY (maquina_id) REFERENCES maquina (id) ON DELETE RESTRICT,
    CONSTRAINT fk_retrabalho_usuario
        FOREIGN KEY (operador_id) REFERENCES operador (id) ON DELETE RESTRICT,
    CONSTRAINT chk_retrabalho_quantidade
        CHECK (qtd_refeita > 0)
);
COMMENT ON TABLE retrabalho IS 'Retrabalho de peças refugadas';

-- ============================================================================
-- 5. CONTROLE DE VIDROS (Ledger Unificado)
-- ============================================================================

CREATE TABLE chapa_vidro
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_vidro     VARCHAR(50)  NOT NULL UNIQUE,
    descricao      VARCHAR(100) NOT NULL,
    estoque_minimo INT          NOT NULL DEFAULT 5,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em  TIMESTAMPTZ  NULL,
    CONSTRAINT chk_chapa_vidro_estoque_minimo
        CHECK (estoque_minimo >= 0)
);
COMMENT ON TABLE chapa_vidro IS 'Catálogo de chapas/tipos de vidro';

CREATE TABLE movimentacao_vidro
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    chapa_vidro_id    BIGINT       NOT NULL,
    tipo_movimentacao VARCHAR(20)  NOT NULL,
    quantidade        INT          NOT NULL,
    observacao        VARCHAR(255) NULL,
    data_hora         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    operador_id       BIGINT       NOT NULL,
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMPTZ  NULL,
    CONSTRAINT fk_movimentacao_chapa_vidro
        FOREIGN KEY (chapa_vidro_id) REFERENCES chapa_vidro (id) ON DELETE RESTRICT,
    CONSTRAINT fk_movimentacao_operador
        FOREIGN KEY (operador_id) REFERENCES operador (id) ON DELETE RESTRICT,
    CONSTRAINT chk_movimentacao_quantidade
        CHECK (quantidade > 0),
    CONSTRAINT chk_movimentacao_tipo
        CHECK (tipo_movimentacao IN ('ENTRADA', 'SAIDA', 'PERDA', 'AJUSTE'))
);
COMMENT ON TABLE movimentacao_vidro IS 'Ledger de entradas/saídas/perdas de vidro vinculado a operador';

-- ============================================================================
-- 6. DADOS INICIAIS
-- ============================================================================

INSERT INTO nivel_acesso (descricao)
VALUES ('SUPORTE'),
       ('GESTAO'),
       ('PCP_SUPERVISOR'),
       ('OPERADOR')
ON CONFLICT DO NOTHING;