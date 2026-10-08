package br.com.lumiflow.dto.ordens;

import br.com.lumiflow.entity.enums.TipoEventoOrdem;

import java.time.LocalDateTime;

public record HistoricoOrdemDTO(
        TipoEventoOrdem tipo,
        String titulo,
        String detalhe,
        LocalDateTime dataHora
) {
}