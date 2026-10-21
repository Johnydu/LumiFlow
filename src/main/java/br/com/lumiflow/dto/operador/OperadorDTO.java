package br.com.lumiflow.dto.operador;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record OperadorDTO(


        Long id,

        @NotBlank
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,


        @Size(max = 30, message = "A função deve ter no máximo 30 caracteres")
        String funcao,

        Long setorPadraoId
) {
}
