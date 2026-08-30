package br.com.lumiflow.dto.ordens;

import br.com.lumiflow.entity.enums.EstatusOrdemProducao;

import java.time.LocalDateTime;

public record OrdemListagemDTO(
        Long id,
        String numero,
        String produtoNome,
        String produtoCodigo,
        Integer quantidade,
        Integer produzido,
        Double percentual,
        String observacao,
        LocalDateTime criadoEm,
        EstatusOrdemProducao status
) {
}