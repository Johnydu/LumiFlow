package br.com.lumiflow.entity.enums;

public enum EstatusOrdemProducao {
    DISPONIVEL,   // chegou no setor, aguardando o PCP liberar
    LIBERADA,     // PCP decidiu que vai ser produzida hoje
    EM_ANDAMENTO, // operador já lançou alguma produção parcial
    CONCLUIDA     // quantidade total foi lançada, pronta pra seguir pro próximo setor
}
