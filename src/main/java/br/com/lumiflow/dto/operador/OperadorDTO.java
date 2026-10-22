package br.com.lumiflow.dto.operador;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record OperadorDTO(


        Long id,

        @NotBlank
        @Size(max = 100, message = "validation.name.size100")
        String nome,


        @Size(max = 30, message = "validation.role.size30")
        String funcao,

        Long setorPadraoId
) {
}
