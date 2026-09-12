package br.com.lumiflow.dto.ordens;

import br.com.lumiflow.entity.enums.EstatusOrdemProducao;

import java.time.LocalDateTime;

public record OrdemListagemDTO(
        Long id,

        Long ordemSetorId,

        String numero,

        String produtoNome,

        String produtoCodigo,

        String etapaNome,

        String variante,

        Integer quantidade,

        Integer produzido,

        Double percentual,

        String observacao,

        LocalDateTime criadoEm,

        EstatusOrdemProducao status
) {}