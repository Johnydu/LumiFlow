package br.com.lumiflow.dto.ordens;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OrdenListaDTO(

        @NotNull
        Long id,

        @NotBlank
        String numero,

        @NotNull
        Integer quantidade,

        @NotBlank
        String status,

        @NotNull
        Integer produtoId,


        @NotEmpty
        String dataHora


) {
}
