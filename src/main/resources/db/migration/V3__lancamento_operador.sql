-- V3__lancamento_operador.sql

ALTER TABLE lancamento DROP COLUMN usuario_id;
ALTER TABLE lancamento ADD COLUMN operador_id BIGINT NOT NULL REFERENCES operador(id);