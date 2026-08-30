

ALTER TABLE ordem_producao ADD COLUMN observacao VARCHAR(500);

CREATE SEQUENCE ordem_producao_numero_seq START WITH 1 INCREMENT BY 1;