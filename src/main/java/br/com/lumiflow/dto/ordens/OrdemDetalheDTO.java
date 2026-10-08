package br.com.lumiflow.dto.ordens;

import br.com.lumiflow.entity.enums.EstatusOrdemProducao;

import java.time.LocalDateTime;
import java.util.List;

public record OrdemDetalheDTO(
        Long id,
        String numero,
        String produtoNome,
        String produtoCodigo,
        Integer quantidade,
        LocalDateTime dataCriacao,
        String observacao,
        EstatusOrdemProducao status,
        int produzidoFinal,
        int descartado,
        double percentual,
        List<RoteiroPassoDetalheDTO> roteiro,
        List<EtapaOrdemDetalheDTO> etapas,
        List<HistoricoOrdemDTO> historico
) {
}