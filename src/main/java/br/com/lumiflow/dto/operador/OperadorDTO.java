package br.com.lumiflow.dto.operador;

import jakarta.validation.constraints.NotNull;

public record OperadorDTO(

        Long id,

        @NotNull
        String nome,


        String funcao,

        Long setorPadraoId
) {
}
