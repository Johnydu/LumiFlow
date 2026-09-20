package br.com.lumiflow.dto.lancamento;

import java.time.LocalDateTime;

public record HistoricoLancamentosDTO(
        String setor,
        String etapa,
        Integer quantidadeProduzida,
        String detalhes,
        String operador,
        LocalDateTime dataHora
) {
}