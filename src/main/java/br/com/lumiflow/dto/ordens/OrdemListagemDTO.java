package br.com.lumiflow.dto.ordens;

import br.com.lumiflow.entity.enums.EstatusOrdemProducao;

import java.time.LocalDateTime;

public record OrdemListagemDTO(
        Long id,
        String numero,
        String produtoNome,
        Integer quantidade,
        Integer produzido,
        Double percentual,
        String setorAtual,
        LocalDateTime criadoEm,
        EstatusOrdemProducao status
) {
}