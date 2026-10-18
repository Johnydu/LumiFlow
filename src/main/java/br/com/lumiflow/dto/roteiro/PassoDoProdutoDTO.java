package br.com.lumiflow.dto.roteiro;

public record PassoDoProdutoDTO(
        Long produtoId,
        Integer sequencia,
        String etapaNome
) {
}
