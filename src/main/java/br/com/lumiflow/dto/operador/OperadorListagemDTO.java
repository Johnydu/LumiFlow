package br.com.lumiflow.dto.operador;

public record OperadorListagemDTO(

        Long id,

        String nome,

        String funcao,

        Long setorPadraoId,

        String setorPadraoNome
) {
}
