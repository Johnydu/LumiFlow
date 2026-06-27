CREATE DATABASE IF NOT EXISTS lumiflow
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE lumiflow;

-- =====================================================
-- SEGURANÇA
-- =====================================================

CREATE TABLE nivel_acesso
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    descricao     VARCHAR(50) NOT NULL,

    criado_em     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP   NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE setor
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    possui_etapas BOOLEAN      NOT NULL DEFAULT FALSE,

    criado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE usuario
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(100) NOT NULL,
    login           VARCHAR(50)  NOT NULL UNIQUE,
    senha           VARCHAR(255) NOT NULL,

    nivel_acesso_id BIGINT       NOT NULL,
    setor_id        BIGINT       NULL,

    criado_em       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

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

CREATE TABLE maquina
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,

    setor_id      BIGINT       NOT NULL,

    criado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_maquina_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
);

CREATE TABLE etapa_setor
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    ordem         INT          NOT NULL,

    setor_id      BIGINT       NOT NULL,

    criado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_etapa_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
);

CREATE TABLE produto
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    descricao     VARCHAR(255),

    criado_em     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP    NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE roteiro_produto
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    sequencia  INT    NOT NULL,

    produto_id BIGINT NOT NULL,
    setor_id   BIGINT NOT NULL,

    CONSTRAINT fk_roteiro_produto
        FOREIGN KEY (produto_id)
            REFERENCES produto (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_roteiro_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
);

-- =====================================================
-- ORDENS DE PRODUÇÃO
-- =====================================================

CREATE TABLE ordem_producao
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero        VARCHAR(30) NOT NULL UNIQUE,

    quantidade    INT         NOT NULL,
    status        VARCHAR(30) NOT NULL,

    produto_id    BIGINT      NOT NULL,
    criado_por_id BIGINT      NOT NULL,

    data_criacao  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    criado_em     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP   NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ordem_produto
        FOREIGN KEY (produto_id)
            REFERENCES produto (id),

    CONSTRAINT fk_ordem_criado_por
        FOREIGN KEY (criado_por_id)
            REFERENCES usuario (id)
);

CREATE TABLE ordem_setor
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,

    sequencia         INT         NOT NULL,

    qtd_recebida      INT         NOT NULL DEFAULT 0,
    qtd_produzida     INT         NOT NULL DEFAULT 0,
    qtd_pendente      INT         NOT NULL DEFAULT 0,

    status            VARCHAR(30) NOT NULL,

    inicio            TIMESTAMP   NULL,
    fim               TIMESTAMP   NULL,

    ordem_producao_id BIGINT      NOT NULL,
    setor_id          BIGINT      NOT NULL,

    criado_em         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMP   NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_ordem_setor_ordem
        FOREIGN KEY (ordem_producao_id)
            REFERENCES ordem_producao (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_ordem_setor_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
);

-- =====================================================
-- LANÇAMENTOS DE PRODUÇÃO
-- =====================================================

CREATE TABLE lancamento
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,

    qtd_produzida  INT       NOT NULL,
    observacao     TEXT,

    data_hora      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    ordem_setor_id BIGINT    NOT NULL,
    maquina_id     BIGINT    NOT NULL,
    usuario_id     BIGINT    NOT NULL,

    etapa_setor_id BIGINT    NULL,

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

CREATE TABLE refugo
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,

    qtd_refugo      INT         NOT NULL,

    motivo          TEXT        NOT NULL,

    destino         VARCHAR(20) NOT NULL,

    data_hora       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    ordem_setor_id  BIGINT      NOT NULL,
    setor_origem_id BIGINT      NOT NULL,
    usuario_id      BIGINT      NOT NULL,

    criado_em       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP   NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

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

CREATE TABLE retrabalho
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,

    qtd_refeita   INT       NOT NULL,
    observacao    TEXT,

    data_hora     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    refugo_id     BIGINT    NOT NULL,
    maquina_id    BIGINT    NOT NULL,
    usuario_id    BIGINT    NOT NULL,

    criado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

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
-- CONTROLE DE VIDRO
-- =====================================================

CREATE TABLE entrada_vidro
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,

    tipo_vidro    VARCHAR(30)    NOT NULL,
    quantidade    DECIMAL(10, 2) NOT NULL,

    observacao    TEXT,

    data_hora     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    usuario_id    BIGINT         NOT NULL,

    criado_em     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_entrada_vidro_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario (id)
);

CREATE TABLE consumo_vidro
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,

    tipo_vidro    VARCHAR(30)    NOT NULL,
    quantidade    DECIMAL(10, 2) NOT NULL,

    observacao    TEXT,

    data_hora     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    usuario_id    BIGINT         NOT NULL,

    criado_em     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP      NULL     DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_consumo_vidro_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario (id)
);