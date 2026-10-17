package br.com.lumiflow.dto.lancamento;

public record HistoricoLancamentoFiltroDTO(
        Long setorId,
        Long etapaSetorId,
        Long operadorId
) {}