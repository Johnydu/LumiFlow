package br.com.lumiflow.dto.lancamento;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RetrabalhoDTO(
        @NotNull(message = "validation.refugo.required")
        Long refugoId,


        Long maquinaId,

        @NotNull(message = "validation.operator.required")
        Long operadorId,

        @NotNull(message = "validation.quantity.required")
        @Min(value = 1, message = "validation.quantity.positive")
        Integer quantidade,

        @Size(max = 255, message = "validation.notes.size255")
        String observacao
) {}
