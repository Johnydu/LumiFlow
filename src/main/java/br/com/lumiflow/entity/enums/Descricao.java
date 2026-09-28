package br.com.lumiflow.entity.enums;

public enum Descricao {
    ADMIN,       // Acesso total ao sistema e configurações de usuários
    SUPERVISOR,  // Líder de produção / encarregado (herda PCP e Gestão)
    PCP,         // Planejamento, engenharia e criação de ordens
    GESTAO,      // Visualização gerencial, relatórios e liberação de ordens
    OPERADOR     // Chão de fábrica (lançamento de produção)
}