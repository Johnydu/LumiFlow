package br.com.lumiflow.dto.roteiro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DuplicarRoteiroDTO(

        @NotBlank(message = "validation.code.required")
        @Size(max = 30, message = "validation.code.size30")
        String codigo
) {
}
