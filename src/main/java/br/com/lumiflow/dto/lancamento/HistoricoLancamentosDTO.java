package br.com.lumiflow.dto.lancamento;

import java.time.LocalDateTime;

public record HistoricoLancamentosDTO(
        Long ordemId,
        String numeroOrdem,
        String produto,
        String setor,
        String etapa,
        String variante,
        String maquina,
        Integer quantidadeProduzida,
        String detalhes,
        String operador,
        LocalDateTime dataHora
) {
}