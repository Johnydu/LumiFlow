-- =====================================================
-- V1__criar_schema_inicial.sql
-- LumiFlow — Schema inicial completo (PostgreSQL)
-- =====================================================

-- -----------------------------------------------------
-- Função genérica para atualizar a coluna atualizado_em
-- -----------------------------------------------------
CREATE OR REPLACE FUNCTION update_atualizado_em_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';


-- =====================================================
-- SEGURANÇA
-- =====================================================

-- Níveis de acesso dos usuários do sistema
CREATE TABLE nivel_acesso
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descricao     VARCHAR(255) NOT NULL UNIQUE,

    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL
);

-- Setores de produção da fábrica
CREATE TABLE setor
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(50) NOT NULL UNIQUE,
    possui_etapas BOOLEAN     NOT NULL DEFAULT FALSE,

    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL
);

-- Usuários do sistema (operadores, supervisores, gestores)
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
        FOREIGN KEY (nivel_acesso_id)
            REFERENCES nivel_acesso (id),

    CONSTRAINT fk_usuario_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
);


-- =====================================================
-- PRODUÇÃO
-- =====================================================

-- Máquinas vinculadas a cada setor
CREATE TABLE maquina
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(50) NOT NULL,
    setor_id      BIGINT      NOT NULL,

    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL,

    CONSTRAINT fk_maquina_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
);

-- Etapas de produção dentro de um setor (ex: 1ª Dobra, 2ª Dobra)
CREATE TABLE etapa_setor
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(50) NOT NULL,
    ordem         INT         NOT NULL,
    setor_id      BIGINT      NOT NULL,

    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL,

    CONSTRAINT fk_etapa_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
);

-- Produtos fabricados
CREATE TABLE produto
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    codigo        VARCHAR(30)  NOT NULL UNIQUE,
    descricao     VARCHAR(255) NULL,

    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NULL
);

-- Roteiro de produção: define quais setores e etapas cada produto percorre
CREATE TABLE roteiro_produto
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sequencia      INT    NOT NULL,
    produto_id     BIGINT NOT NULL,
    setor_id       BIGINT NOT NULL,
    etapa_setor_id BIGINT NULL,

    CONSTRAINT fk_roteiro_produto
        FOREIGN KEY (produto_id)
            REFERENCES produto (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_roteiro_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id),

    CONSTRAINT fk_roteiro_etapa
        FOREIGN KEY (etapa_setor_id)
            REFERENCES etapa_setor (id)
);


-- =====================================================
-- ORDENS DE PRODUÇÃO
-- =====================================================

-- Ordem de produção principal
CREATE TABLE ordem_producao
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero        VARCHAR(255) NOT NULL UNIQUE,
    quantidade    INT          NOT NULL,
    status        VARCHAR(25)  NOT NULL,
    produto_id    BIGINT       NOT NULL,
    criado_por_id BIGINT       NOT NULL,
    data_criacao  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ  NULL,

    CONSTRAINT fk_ordem_produto
        FOREIGN KEY (produto_id)
            REFERENCES produto (id),

    CONSTRAINT fk_ordem_criado_por
        FOREIGN KEY (criado_por_id)
            REFERENCES usuario (id)
);

-- Andamento da ordem em cada setor do roteiro
CREATE TABLE ordem_setor
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sequencia         INT         NOT NULL,
    qtd_recebida      INT         NOT NULL DEFAULT 0,
    qtd_produzida     INT         NOT NULL DEFAULT 0,
    qtd_pendente      INT         NOT NULL DEFAULT 0,
    status            VARCHAR(25) NOT NULL,
    inicio            TIMESTAMPTZ NULL,
    fim               TIMESTAMPTZ NULL,
    ordem_producao_id BIGINT      NOT NULL,
    setor_id          BIGINT      NOT NULL,
    etapa_setor_id    BIGINT      NULL,

    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL,

    CONSTRAINT fk_ordem_setor_ordem
        FOREIGN KEY (ordem_producao_id)
            REFERENCES ordem_producao (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_ordem_setor_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id),

    CONSTRAINT fk_ordem_setor_etapa
        FOREIGN KEY (etapa_setor_id)
            REFERENCES etapa_setor (id)
);

-- Lançamentos de produção realizados pelos operadores
CREATE TABLE lancamento
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    qtd_produzida  INT          NOT NULL,
    observacao     VARCHAR(255) NULL,
    data_hora      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ordem_setor_id BIGINT       NOT NULL,
    maquina_id     BIGINT       NOT NULL,
    usuario_id     BIGINT       NOT NULL,
    etapa_setor_id BIGINT       NULL,

    CONSTRAINT fk_lancamento_ordem_setor
        FOREIGN KEY (ordem_setor_id)
            REFERENCES ordem_setor (id),

    CONSTRAINT fk_lancamento_maquina
        FOREIGN KEY (maquina_id)
            REFERENCES maquina (id),

    CONSTRAINT fk_lancamento_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario (id),

    CONSTRAINT fk_lancamento_etapa
        FOREIGN KEY (etapa_setor_id)
            REFERENCES etapa_setor (id)
);


-- =====================================================
-- QUALIDADE
-- =====================================================

-- Peças refugadas durante a produção
CREATE TABLE refugo
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    qtd_refugo      INT          NOT NULL,
    motivo          VARCHAR(255) NOT NULL,
    destino         VARCHAR(30)  NOT NULL,
    data_hora       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ordem_setor_id  BIGINT       NOT NULL,
    setor_origem_id BIGINT       NOT NULL,
    usuario_id      BIGINT       NOT NULL,

    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL,

    CONSTRAINT fk_refugo_ordem_setor
        FOREIGN KEY (ordem_setor_id)
            REFERENCES ordem_setor (id),

    CONSTRAINT fk_refugo_setor_origem
        FOREIGN KEY (setor_origem_id)
            REFERENCES setor (id),

    CONSTRAINT fk_refugo_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario (id),

    CONSTRAINT chk_refugo_destino
        CHECK (destino IN ('RETRABALHO', 'DESCARTE'))
);

-- Retrabalho de peças refugadas
CREATE TABLE retrabalho
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    qtd_refeita INT          NOT NULL,
    observacao  VARCHAR(255) NULL,
    data_hora   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refugo_id   BIGINT       NOT NULL,
    maquina_id  BIGINT       NOT NULL,
    usuario_id  BIGINT       NOT NULL,

    criado_em     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NULL,

    CONSTRAINT fk_retrabalho_refugo
        FOREIGN KEY (refugo_id)
            REFERENCES refugo (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_retrabalho_maquina
        FOREIGN KEY (maquina_id)
            REFERENCES maquina (id),

    CONSTRAINT fk_retrabalho_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario (id)
);


-- =====================================================
-- CONTROLE DE VIDROS (Ledger Unificado)
-- =====================================================

-- Catálogo de tipos de vidro usados na produção
CREATE TABLE tipo_vidro
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo_produto VARCHAR(50)  NOT NULL UNIQUE,
    descricao      VARCHAR(100) NOT NULL,
    estoque_minimo INT          NOT NULL DEFAULT 5,

    criado_em      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em  TIMESTAMPTZ  NULL
);

-- Ledger de movimentações de vidro (entradas, saídas, perdas, ajustes)
CREATE TABLE movimentacao_vidro
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tipo_vidro_id     BIGINT      NOT NULL,
    tipo_movimentacao VARCHAR(20) NOT NULL,
    quantidade        INT         NOT NULL,
    observacao        VARCHAR(255) NULL,
    data_hora         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_id        BIGINT      NOT NULL,

    criado_em      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em  TIMESTAMPTZ NULL,

    CONSTRAINT chk_movimentacao_quantidade_positiva
        CHECK (quantidade > 0),

    CONSTRAINT fk_movimentacao_tipo_vidro
        FOREIGN KEY (tipo_vidro_id)
            REFERENCES tipo_vidro (id),

    CONSTRAINT fk_movimentacao_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario (id)
);


-- =====================================================
-- TRIGGERS PARA ATUALIZADO_EM
-- =====================================================

CREATE TRIGGER update_nivel_acesso_atualizado_em BEFORE UPDATE ON nivel_acesso FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_setor_atualizado_em BEFORE UPDATE ON setor FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_usuario_atualizado_em BEFORE UPDATE ON usuario FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_maquina_atualizado_em BEFORE UPDATE ON maquina FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_etapa_setor_atualizado_em BEFORE UPDATE ON etapa_setor FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_produto_atualizado_em BEFORE UPDATE ON produto FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_ordem_producao_atualizado_em BEFORE UPDATE ON ordem_producao FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_ordem_setor_atualizado_em BEFORE UPDATE ON ordem_setor FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_refugo_atualizado_em BEFORE UPDATE ON refugo FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_retrabalho_atualizado_em BEFORE UPDATE ON retrabalho FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_tipo_vidro_atualizado_em BEFORE UPDATE ON tipo_vidro FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();
CREATE TRIGGER update_movimentacao_vidro_atualizado_em BEFORE UPDATE ON movimentacao_vidro FOR EACH ROW EXECUTE FUNCTION update_atualizado_em_column();


-- =====================================================
-- ÍNDICES DE PERFORMANCE
-- =====================================================

-- Usuário
CREATE INDEX idx_usuario_nivel_acesso  ON usuario (nivel_acesso_id);
CREATE INDEX idx_usuario_setor          ON usuario (setor_id);

-- Máquina
CREATE INDEX idx_maquina_setor         ON maquina (setor_id);

-- Etapa setor
CREATE INDEX idx_etapa_setor_setor      ON etapa_setor (setor_id);
CREATE INDEX idx_etapa_setor_ordem      ON etapa_setor (setor_id, ordem);

-- Roteiro produto
CREATE INDEX idx_roteiro_produto       ON roteiro_produto (produto_id);
CREATE INDEX idx_roteiro_setor         ON roteiro_produto (setor_id);
CREATE INDEX idx_roteiro_sequencia     ON roteiro_produto (produto_id, sequencia);

-- Ordem de produção
CREATE INDEX idx_ordem_produto         ON ordem_producao (produto_id);
CREATE INDEX idx_ordem_status          ON ordem_producao (status);
CREATE INDEX idx_ordem_criado_por      ON ordem_producao (criado_por_id);

-- Ordem setor
CREATE INDEX idx_ordem_setor_ordem     ON ordem_setor (ordem_producao_id);
CREATE INDEX idx_ordem_setor_setor     ON ordem_setor (setor_id);
CREATE INDEX idx_ordem_setor_status    ON ordem_setor (status);

-- Lançamento
CREATE INDEX idx_lancamento_ordem      ON lancamento (ordem_setor_id);
CREATE INDEX idx_lancamento_maquina    ON lancamento (maquina_id);
CREATE INDEX idx_lancamento_usuario    ON lancamento (usuario_id);
CREATE INDEX idx_lancamento_data       ON lancamento (data_hora);

-- Refugo
CREATE INDEX idx_refugo_ordem_setor    ON refugo (ordem_setor_id);
CREATE INDEX idx_refugo_setor_origem   ON refugo (setor_origem_id);

-- Retrabalho
CREATE INDEX idx_retrabalho_refugo     ON retrabalho (refugo_id);

-- Movimentação vidro
CREATE INDEX idx_movimentacao_tipo     ON movimentacao_vidro (tipo_vidro_id, data_hora);
CREATE INDEX idx_movimentacao_data     ON movimentacao_vidro (data_hora);
CREATE INDEX idx_movimentacao_usuario  ON movimentacao_vidro (usuario_id);


-- =====================================================
-- DADOS INICIAIS
-- =====================================================

-- Níveis de acesso
INSERT INTO nivel_acesso (descricao) VALUES
                                         ('SUPORTE'),
                                         ('GESTAO'),
                                         ('PCP_SUPERVISOR'),
                                         ('OPERADOR');
