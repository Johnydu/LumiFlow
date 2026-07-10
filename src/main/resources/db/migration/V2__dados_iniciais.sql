-- =====================================================
-- NÍVEIS DE ACESSO
-- =====================================================

INSERT INTO nivel_acesso (descricao)
VALUES
    ('SUPORTE'),
    ('GESTAO'),
    ('PCP_SUPERVISOR'),
    ('OPERADOR');

-- =====================================================
-- SETORES INICIAIS
-- =====================================================

INSERT INTO setor (nome, possui_etapas)
VALUES
    ('PCP', FALSE),
    ('CORTE', TRUE),
    ('COLAGEM', FALSE),
    ('PINTURA', FALSE),
    ('LIMPEZA', FALSE),
    ('DOBRA', TRUE);



INSERT INTO usuario (
    nome,
    login,
    senha,
    nivel_acesso_id,
    setor_id
)
VALUES (
           'Administrador',
           'admin',
           '$2a$12$RiHd1lmkeni2UnTgZmNp9uXjlzI00KZUFWqZBolnzRh/OOrfKXUUS',
           1,
           NULL
       );