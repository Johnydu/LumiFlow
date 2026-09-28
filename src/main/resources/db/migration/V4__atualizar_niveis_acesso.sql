-- ============================================================
-- Migration: Atualização e inserção dos níveis de acesso
-- ============================================================

-- 1. Atualiza o antigo PCP_SUPERVISOR para SUPERVISOR
UPDATE nivel_acesso
SET descricao = 'SUPERVISOR'
WHERE descricao = 'PCP_SUPERVISOR';

-- 2. Atualiza o antigo SUPORTE para ADMIN
UPDATE nivel_acesso
SET descricao = 'ADMIN'
WHERE descricao = 'SUPORTE';

INSERT INTO nivel_acesso (descricao)
VALUES ('PCP')
ON CONFLICT (descricao) DO NOTHING;