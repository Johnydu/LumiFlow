package br.com.lumiflow.dto.roteiro;

import java.util.List;

public record RoteiroListagemDTO(

        Long produtoId,

        String nomeProduto,

        String codigoProduto,

        Integer quantidadeEtapas,

        List<String> etapas

) {
}