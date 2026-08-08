package br.com.lumiflow.dto.operador;

public record OperadorDTO(

        Long id,

        String nome,

        String funcao,

        Long setorPadraoId
) {
}
