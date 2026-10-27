package br.com.lumiflow.dto.dashboard;

import br.com.lumiflow.entity.enums.EstatusOrdemProducao;

public record DashboardOrdemDTO(
        Long id,
        String numero,
        String produtoNome,
        String produtoCodigo,
        Integer quantidade,
        Double progresso,
        int etapasConcluidas,
        int totalEtapas,
        String etapaAtualNome,
        String setorAtualNome,
        EstatusOrdemProducao status
) {}
