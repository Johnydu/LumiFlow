package br.com.lumiflow.dto.dashboard;

public record DashboardOrdemDTO(
        Long id,
        String numero,
        String produtoNome,
        String produtoCodigo,
        Integer quantidade,
        Double progresso,
        String etapaAtualNome,
        String setorAtualNome,
        String status
) {}