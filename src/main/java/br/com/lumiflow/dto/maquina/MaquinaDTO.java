package br.com.lumiflow.dto.maquina;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaquinaDTO(

        Long id,

        @NotBlank(message = "validation.machine.name.required")
        @Size(max = 50, message = "validation.name.size50")
        String nome,

        String setor,

        @NotNull(message = "validation.sector.required")
        Long setorId
) {
}
