package br.com.lumiflow.entity.enums;

public enum SituacaoPasso {
    CONCLUIDO,   // todas as linhas do passo concluídas e todos os passos anteriores concluídos
    ATUAL,       // passo com linhas em andamento ou aguardando liberação
    AGUARDANDO   // passo que a ordem ainda não alcançou
}