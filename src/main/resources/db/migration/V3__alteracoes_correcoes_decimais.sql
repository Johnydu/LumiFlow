-- 1. ADIÇÃO DO CÓDIGO DO PRODUTO (Correto como você fez)
ALTER TABLE produto ADD codigo VARCHAR(30) NULL;
ALTER TABLE produto MODIFY codigo VARCHAR(30) NOT NULL;
ALTER TABLE produto ADD CONSTRAINT uc_produto_codigo UNIQUE (codigo);

-- 2. AJUSTES DA SUA MIGRATION (Com os tamanhos de texto mais seguros)
ALTER TABLE nivel_acesso ADD CONSTRAINT uc_nivel_acesso_descricao UNIQUE (descricao);
ALTER TABLE entrada_vidro DROP COLUMN atualizado_em;
ALTER TABLE entrada_vidro DROP COLUMN criado_em;
ALTER TABLE nivel_acesso MODIFY descricao VARCHAR(255);
ALTER TABLE refugo MODIFY destino VARCHAR(30);
ALTER TABLE usuario MODIFY login VARCHAR(20);
ALTER TABLE refugo MODIFY motivo VARCHAR(255);

-- Corrigido: Aumentado de 20 para 50 para evitar erro ao digitar nomes longos de etapas
ALTER TABLE etapa_setor MODIFY nome VARCHAR(50);
-- Corrigido: Aumentado de 20 para 50
ALTER TABLE maquina MODIFY nome VARCHAR(50);
-- Corrigido: Aumentado de 30 para 100 (Nome de produto costuma ser longo)
ALTER TABLE produto MODIFY nome VARCHAR(100);
-- Corrigido: Aumentado de 25 para 50
ALTER TABLE setor MODIFY nome VARCHAR(50);

ALTER TABLE usuario MODIFY nome VARCHAR(50);
ALTER TABLE ordem_producao MODIFY numero VARCHAR(255);
ALTER TABLE consumo_vidro MODIFY observacao VARCHAR(255);
ALTER TABLE entrada_vidro MODIFY observacao VARCHAR(255);
ALTER TABLE lancamento MODIFY observacao VARCHAR(255);
ALTER TABLE retrabalho MODIFY observacao VARCHAR(255);
ALTER TABLE setor MODIFY possui_etapas BIT(1) NULL;

-- Corrigido: Mantendo as casas decimais para as medidas do vidro
ALTER TABLE consumo_vidro MODIFY quantidade DECIMAL(10,2);
ALTER TABLE entrada_vidro MODIFY quantidade DECIMAL(10,2);

ALTER TABLE ordem_producao MODIFY status VARCHAR(25);
ALTER TABLE ordem_setor MODIFY status VARCHAR(25);
ALTER TABLE consumo_vidro MODIFY tipo_vidro VARCHAR(25);
ALTER TABLE entrada_vidro MODIFY tipo_vidro VARCHAR(25);

-- ==========================================================
-- 3. O QUE FALTAVA PARA A NOSSA LÓGICA DE ROTEIRO FUNCIONAR
-- ==========================================================

-- Adiciona a Etapa no roteiro do produto
ALTER TABLE roteiro_produto
    ADD COLUMN etapa_setor_id BIGINT NULL AFTER setor_id,
ADD CONSTRAINT fk_roteiro_etapa
    FOREIGN KEY (etapa_setor_id)
    REFERENCES etapa_setor (id);

-- Adiciona a Etapa no andamento da OP
ALTER TABLE ordem_setor
    ADD COLUMN etapa_setor_id BIGINT NULL AFTER setor_id,
ADD CONSTRAINT fk_ordem_setor_etapa
    FOREIGN KEY (etapa_setor_id)
    REFERENCES etapa_setor (id);