package br.com.lumiflow.dto.roteiro;

import java.util.List;

public record RoteiroListagemDTO(
        Long id,
        String codigo,
        String descricao,
        Integer quantidadeEtapas,
        List<String> etapas,
        long quantidadeProdutos
) {
}
