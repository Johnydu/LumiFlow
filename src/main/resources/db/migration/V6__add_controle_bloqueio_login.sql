-- ============================================================================
-- Proteção contra força bruta: controle de tentativas e bloqueio temporário
-- ============================================================================

ALTER TABLE usuario
    ADD COLUMN tentativas_falhas INT         NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_ate     TIMESTAMPTZ NULL;

ALTER TABLE usuario
    ADD CONSTRAINT chk_usuario_tentativas_falhas CHECK (tentativas_falhas >= 0);

COMMENT ON COLUMN usuario.tentativas_falhas IS 'Falhas consecutivas de login desde o último sucesso ou bloqueio';
COMMENT ON COLUMN usuario.bloqueado_ate IS 'Login bloqueado temporariamente até este instante (NULL = sem bloqueio)';