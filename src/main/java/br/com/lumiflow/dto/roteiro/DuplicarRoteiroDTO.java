package br.com.lumiflow.dto.roteiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DuplicarRoteiroDTO(

        @NotBlank(message = "O código é obrigatório")
        @Size(max = 30, message = "O código deve ter no máximo 30 caracteres")
        String codigo
) {
}
