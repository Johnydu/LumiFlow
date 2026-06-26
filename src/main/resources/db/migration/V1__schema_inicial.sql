CREATE DATABASE lumiflow;
USE lumiflow;

-- =========================
-- SEGURANÇA
-- =========================

CREATE TABLE nivel_acesso
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    descricao VARCHAR(50) NOT NULL
);

CREATE TABLE setor
(
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL
);

CREATE TABLE usuario
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(100) NOT NULL,
    login           VARCHAR(50)  NOT NULL UNIQUE,
    senha           VARCHAR(255) NOT NULL,
    criado_em       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    nivel_acesso_id BIGINT       NOT NULL,
    setor_id        BIGINT       NOT NULL,

    CONSTRAINT fk_usuario_nivel_acesso
        FOREIGN KEY (nivel_acesso_id)
            REFERENCES nivel_acesso (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT,

    CONSTRAINT fk_usuario_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);

-- =========================
-- PRODUÇÃO
-- =========================

CREATE TABLE maquina
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome     VARCHAR(100) NOT NULL,

    setor_id BIGINT       NOT NULL,

    CONSTRAINT fk_maquina_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);

CREATE TABLE etapa_setor
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome     VARCHAR(100) NOT NULL,
    ordem    INT          NOT NULL,

    setor_id BIGINT       NOT NULL,

    CONSTRAINT fk_etapa_setor
        FOREIGN KEY (setor_id)
            REFERENCES setor (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);

CREATE TABLE produto
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome      VARCHAR(100) NOT NULL,
    descricao VARCHAR(255)
);

CREATE TABLE roteiro_produto
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sequencia      INT    NOT NULL,

    produto_id     BIGINT NOT NULL,
    etapa_setor_id BIGINT NOT NULL,

    CONSTRAINT fk_roteiro_produto
        FOREIGN KEY (produto_id)
            REFERENCES produto (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT fk_roteiro_etapa
        FOREIGN KEY (etapa_setor_id)
            REFERENCES etapa_setor (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);

-- =========================
-- ORDENS DE PRODUÇÃO
-- =========================

CREATE TABLE ordem_producao
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero       VARCHAR(30) NOT NULL UNIQUE,
    data_criacao TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    quantidade   INT         NOT NULL,

    status       VARCHAR(30) NOT NULL,

    produto_id   BIGINT      NOT NULL,

    CONSTRAINT fk_ordem_produto
        FOREIGN KEY (produto_id)
            REFERENCES produto (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);

CREATE TABLE ordem_setor
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    sequencia         INT         NOT NULL,

    qtd_recebida      INT         NOT NULL DEFAULT 0,
    qtd_produzida     INT         NOT NULL DEFAULT 0,

    status            VARCHAR(30) NOT NULL,

    inicio            TIMESTAMP   NULL,
    fim               TIMESTAMP   NULL,

    ordem_producao_id BIGINT      NOT NULL,
    etapa_setor_id    BIGINT      NOT NULL,

    CONSTRAINT fk_ordem_setor_op
        FOREIGN KEY (ordem_producao_id)
            REFERENCES ordem_producao (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE,

    CONSTRAINT fk_ordem_setor_etapa
        FOREIGN KEY (etapa_setor_id)
            REFERENCES etapa_setor (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);

-- =========================
-- PRODUÇÃO
-- =========================

CREATE TABLE lancamento
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    qtd_produzida  INT       NOT NULL,
    data_hora      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacao     TEXT,

    ordem_setor_id BIGINT    NOT NULL,
    maquina_id     BIGINT    NOT NULL,
    usuario_id     BIGINT    NOT NULL,

    CONSTRAINT fk_lancamento_ordem_setor
        FOREIGN KEY (ordem_setor_id)
            REFERENCES ordem_setor (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT,

    CONSTRAINT fk_lancamento_maquina
        FOREIGN KEY (maquina_id)
            REFERENCES maquina (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT,

    CONSTRAINT fk_lancamento_usuario
        FOREIGN KEY (usuario_id)
            REFERENCES usuario (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);

-- =========================
-- QUALIDADE
-- =========================

CREATE TABLE refugo
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_hora      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    destino        VARCHAR(100) NOT NULL,
    qtd_refugo     INT          NOT NULL,

    ordem_setor_id BIGINT       NOT NULL,

    CONSTRAINT fk_refugo_ordem_setor
        FOREIGN KEY (ordem_setor_id)
            REFERENCES ordem_setor (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);

CREATE TABLE retrabalho
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    qtd_refeita INT       NOT NULL,
    data_hora   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacao  TEXT,

    refugo_id   BIGINT    NOT NULL,

    CONSTRAINT fk_retrabalho_refugo
        FOREIGN KEY (refugo_id)
            REFERENCES refugo (id)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

-- =========================
-- MATERIA PRIMA (VIDRO)
-- =========================

CREATE TABLE entrada_vidro
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,

    tipo_vidro VARCHAR(30)    NOT NULL,

    quantidade DECIMAL(10, 2) NOT NULL,
    data_hora  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consumo_vidro
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,

    tipo_vidro     VARCHAR(30)    NOT NULL,

    quantidade     DECIMAL(10, 2) NOT NULL,
    data_hora      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    ordem_setor_id BIGINT         NOT NULL,

    CONSTRAINT fk_consumo_ordem_setor
        FOREIGN KEY (ordem_setor_id)
            REFERENCES ordem_setor (id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT
);