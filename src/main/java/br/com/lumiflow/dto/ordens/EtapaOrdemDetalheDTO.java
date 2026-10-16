package br.com.lumiflow.dto.ordens;

import br.com.lumiflow.entity.enums.EstatusOrdemProducao;

public record EtapaOrdemDetalheDTO(
        Long ordemSetorId,
        Long setorId,
        String setor,
        String etapa,
        String variante,
        boolean origemRefugo,
        Integer recebido,
        Integer produzido,
        Integer pendente,
        double percentual,
        EstatusOrdemProducao status
) {
}