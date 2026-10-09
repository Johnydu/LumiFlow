ALTER TABLE etapa_setor DROP CONSTRAINT uq_etapa_setor_ordem;

ALTER TABLE etapa_setor
    ADD CONSTRAINT uq_etapa_setor_ordem UNIQUE (setor_id, ordem) DEFERRABLE INITIALLY DEFERRED;
