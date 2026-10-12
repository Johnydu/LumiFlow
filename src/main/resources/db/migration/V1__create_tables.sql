-- ============================================================================
-- V1 — ESTRUTURA DO BANCO: TABELAS, CHAVES, RELACIONAMENTOS E REGRAS (CHECK)
-- ============================================================================
-- Convenções de nomes:
--   fk_<tabela>_<referenciada> | uq_<tabela>_<coluna> | chk_<tabela>_<regra>
--
-- Regras:
--   * Constraints (PK, FK, UNIQUE, CHECK) ficam aqui, junto da tabela.
--   * Triggers e funções ficam na V2; índices de desempenho ficam na V3.
--   * Valores de CHECK espelham os enums Java. Ao criar um valor novo no enum,
--     atualize o CHECK correspondente em uma nova migration.
-- ============================================================================

-- ============================================================================
-- 1. SEGURANÇA E CONTROLE DE ACESSO
-- ============================================================================

CREATE TABLE nivel_acesso
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descricao     VARCHAR(30) NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL,
    CONSTRAINT uq_nivel_acesso_descricao
        UNIQUE (descricao),
    CONSTRAINT chk_nivel_acesso_descricao
        CHECK (descricao IN ('ADMIN', 'GESTAO', 'SUPERVISOR', 'OPERADOR', 'PCP'))
);
COMMENT ON TABLE nivel_acesso IS 'Perfis de acesso: ADMIN, SUPERVISOR, PCP, GESTAO e OPERADOR (hierarquia definida no SecurityConfig)';

CREATE TABLE setor
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(50) NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL,
    CONSTRAINT uq_setor_nome
        UNIQUE (nome)
);
COMMENT ON TABLE setor IS 'Setores de produção (ex: Corte, Dobra, Pintura)';

CREATE TABLE usuario
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome              VARCHAR(50)  NOT NULL,
    login             VARCHAR(20)  NOT NULL,
    senha             VARCHAR(255) NOT NULL,
    nivel_acesso_id   BIGINT       NOT NULL,
    setor_id          BIGINT       NULL,
    tentativas_falhas INT          NOT NULL DEFAULT 0,
    bloqueado_ate     TIMESTAMPTZ  NULL,
    criado_em         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMPTZ  NULL,
    CONSTRAINT uq_usuario_login
        UNIQUE (login),
    CONSTRAINT fk_usuario_nivel_acesso
        FOREIGN KEY (nivel_acesso_id) REFERENCES nivel_acesso (id),
    CONSTRAINT fk_usuario_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE SET NULL,
    CONSTRAINT chk_usuario_tentativas_falhas
        CHECK (tentativas_falhas >= 0)
);
COMMENT ON TABLE usuario IS 'Usuários do sistema (login). Quem executa a produção é o operador, não o usuário';
COMMENT ON COLUMN usuario.tentativas_falhas IS 'Falhas consecutivas de login desde o último sucesso ou bloqueio';
COMMENT ON COLUMN usuario.bloqueado_ate IS 'Login bloqueado temporariamente até este instante (NULL = sem bloqueio)';

CREATE TABLE operador
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome            VARCHAR(100) NOT NULL,
    funcao          VARCHAR(30)  NULL,
    setor_padrao_id BIGINT       NULL,
    criado_em       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMPTZ  NULL,
    CONSTRAINT uq_operador_nome
        UNIQUE (nome),
    CONSTRAINT fk_operador_setor_padrao
        FOREIGN KEY (setor_padrao_id) REFERENCES setor (id) ON DELETE SET NULL
);
COMMENT ON TABLE operador IS 'Operadores de produção, escolhidos nos formulários de lançamento';
COMMENT ON COLUMN operador.setor_padrao_id IS 'Setor de atuação (NULL = volante)';

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
    CONSTRAINT uq_maquina_nome
        UNIQUE (nome),
    CONSTRAINT fk_maquina_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE RESTRICT
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
    CONSTRAINT chk_etapa_setor_ordem
        CHECK (ordem > 0),
    -- DEFERRABLE: a unicidade só é verificada no commit. Permite renumerar e
    -- remover etapas na mesma transação sem violação intermediária.
    CONSTRAINT uq_etapa_setor_ordem
        UNIQUE (setor_id, ordem) DEFERRABLE INITIALLY DEFERRED
);
COMMENT ON TABLE etapa_setor IS 'Etapas sequenciais dentro de cada setor';

CREATE TABLE produto
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    codigo        VARCHAR(30)  NOT NULL,
    descricao     VARCHAR(500) NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NULL,
    CONSTRAINT uq_produto_codigo
        UNIQUE (codigo),
    CONSTRAINT uq_produto_nome
        UNIQUE (nome)
);
COMMENT ON TABLE produto IS 'Catálogo de produtos fabricados';

-- O roteiro é substituído por inteiro a cada edição (apaga e recria os passos),
-- por isso a tabela não tem colunas de auditoria nem trigger.
CREATE TABLE roteiro_produto
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sequencia      INT    NOT NULL,
    produto_id     BIGINT NOT NULL,
    setor_id       BIGINT NOT NULL,
    etapa_setor_id BIGINT NULL,
    CONSTRAINT uq_roteiro_sequencia
        UNIQUE (produto_id, sequencia),
    CONSTRAINT chk_roteiro_sequencia
        CHECK (sequencia > 0),
    CONSTRAINT fk_roteiro_produto
        FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE CASCADE,
    CONSTRAINT fk_roteiro_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE RESTRICT,
    CONSTRAINT fk_roteiro_etapa
        FOREIGN KEY (etapa_setor_id) REFERENCES etapa_setor (id) ON DELETE SET NULL
);
COMMENT ON TABLE roteiro_produto IS 'Define o percurso de cada produto pela produção';

-- ============================================================================
-- 3. ORDENS DE PRODUÇÃO
-- ============================================================================

CREATE SEQUENCE ordem_producao_numero_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE ordem_producao
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero        VARCHAR(30)  NOT NULL,
    quantidade    INT          NOT NULL,
    status        VARCHAR(25)  NOT NULL,
    produto_id    BIGINT       NOT NULL,
    criado_por_id BIGINT       NOT NULL,
    observacao    VARCHAR(500) NULL,
    data_criacao  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NULL,
    CONSTRAINT uq_ordem_producao_numero
        UNIQUE (numero),
    CONSTRAINT fk_ordem_produto
        FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE RESTRICT,
    CONSTRAINT fk_ordem_criado_por
        FOREIGN KEY (criado_por_id) REFERENCES usuario (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ordem_quantidade
        CHECK (quantidade > 0),
    CONSTRAINT chk_ordem_status
        CHECK (status IN ('DISPONIVEL', 'LIBERADA', 'EM_ANDAMENTO', 'CONCLUIDA'))
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
    versao            BIGINT      NOT NULL DEFAULT 0,
    ordem_producao_id BIGINT      NOT NULL,
    setor_id          BIGINT      NOT NULL,
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMPTZ NULL,
    CONSTRAINT fk_ordem_setor_ordem
        FOREIGN KEY (ordem_producao_id) REFERENCES ordem_producao (id) ON DELETE CASCADE,
    CONSTRAINT fk_ordem_setor_setor
        FOREIGN KEY (setor_id) REFERENCES setor (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ordem_setor_quantidades
        CHECK (qtd_recebida >= 0 AND qtd_produzida >= 0 AND qtd_pendente >= 0),
    CONSTRAINT chk_ordem_setor_status
        CHECK (status IN ('DISPONIVEL', 'LIBERADA', 'EM_ANDAMENTO', 'CONCLUIDA'))
);
COMMENT ON TABLE ordem_setor IS 'Linha de andamento da ordem em um setor (uma por sequência e variante, mais as de refugo)';
COMMENT ON COLUMN ordem_setor.variante IS 'Variante/cor criada pela divisão da etapa (NULL = sem divisão)';
COMMENT ON COLUMN ordem_setor.origem_refugo IS 'TRUE = linha criada por um refugo para retrabalho';
COMMENT ON COLUMN ordem_setor.versao IS 'Controle de concorrência otimista (@Version)';

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
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    qtd_refugo                INT          NOT NULL,
    motivo                    VARCHAR(255) NOT NULL,
    destino                   VARCHAR(30)  NOT NULL,
    variante                  VARCHAR(50)  NULL,
    data_hora                 TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sequencia_origem          INT          NOT NULL,
    ordem_setor_id            BIGINT       NOT NULL,
    setor_origem_id           BIGINT       NOT NULL,
    operador_id               BIGINT       NOT NULL,
    ordem_setor_retrabalho_id BIGINT       NULL,
    criado_em                 TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em             TIMESTAMPTZ  NULL,
    CONSTRAINT fk_refugo_ordem_setor
        FOREIGN KEY (ordem_setor_id) REFERENCES ordem_setor (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refugo_setor_origem
        FOREIGN KEY (setor_origem_id) REFERENCES setor (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refugo_operador
        FOREIGN KEY (operador_id) REFERENCES operador (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refugo_ordem_setor_retrabalho
        FOREIGN KEY (ordem_setor_retrabalho_id) REFERENCES ordem_setor (id) ON DELETE SET NULL,
    CONSTRAINT chk_refugo_destino
        CHECK (destino IN ('RETRABALHO', 'DESCARTE')),
    CONSTRAINT chk_refugo_quantidade
        CHECK (qtd_refugo > 0)
);
COMMENT ON TABLE refugo IS 'Registro de peças refugadas com motivo e destino';
COMMENT ON COLUMN refugo.ordem_setor_id IS 'Linha onde o refugo foi detectado';
COMMENT ON COLUMN refugo.ordem_setor_retrabalho_id IS 'Linha criada no setor de origem para o retrabalho (NULL no descarte)';

CREATE TABLE retrabalho
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    qtd_refeita   INT          NOT NULL,
    observacao    VARCHAR(255) NULL,
    data_hora     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refugo_id     BIGINT       NOT NULL,
    maquina_id    BIGINT       NULL,
    operador_id   BIGINT       NOT NULL,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NULL,
    CONSTRAINT fk_retrabalho_refugo
        FOREIGN KEY (refugo_id) REFERENCES refugo (id) ON DELETE CASCADE,
    CONSTRAINT fk_retrabalho_maquina
        FOREIGN KEY (maquina_id) REFERENCES maquina (id) ON DELETE RESTRICT,
    CONSTRAINT fk_retrabalho_operador
        FOREIGN KEY (operador_id) REFERENCES operador (id) ON DELETE RESTRICT,
    CONSTRAINT chk_retrabalho_quantidade
        CHECK (qtd_refeita > 0)
);
COMMENT ON TABLE retrabalho IS 'Retrabalho de peças refugadas';

-- ============================================================================
-- 5. CONTROLE DE VIDROS (ledger de movimentações)
-- ============================================================================

CREATE TABLE chapa_vidro
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_vidro     VARCHAR(20)  NOT NULL,
    descricao      VARCHAR(100) NOT NULL,
    estoque_minimo INT          NOT NULL DEFAULT 5,
    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em  TIMESTAMPTZ  NULL,
    CONSTRAINT uq_chapa_vidro_tipo
        UNIQUE (tipo_vidro),
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
        CHECK (tipo_movimentacao IN ('ENTRADA', 'SAIDA'))
);
COMMENT ON TABLE movimentacao_vidro IS 'Ledger de entradas e saídas de vidro; o saldo é a soma das movimentações';

-- ============================================================================
-- 6. DADOS INICIAIS
-- ============================================================================
-- A ordem dos INSERTs define os ids (ADMIN=1 ... PCP=5) e preserva os ids de
-- bancos criados pelo histórico antigo de migrations.

INSERT INTO nivel_acesso (descricao)
VALUES ('ADMIN'),
       ('GESTAO'),
       ('SUPERVISOR'),
       ('OPERADOR'),
       ('PCP');