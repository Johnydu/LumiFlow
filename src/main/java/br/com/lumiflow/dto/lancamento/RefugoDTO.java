package br.com.lumiflow.dto.lancamento;

import br.com.lumiflow.entity.enums.DestinoRefugo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RefugoDTO(
        @NotNull(message = "validation.quantity.required")
        @Min(value = 1, message = "validation.quantity.positive")
        Integer quantidade,

        @NotNull(message = "validation.sector.origin.required")
        Long setorOrigemId,

        Long etapaSetorId,

        @NotNull(message = "validation.operator.required")
        Long operadorId,

        @NotNull(message = "validation.destination.required")
        DestinoRefugo destino,

        @NotBlank(message = "validation.reason.required")
        @Size(max = 255, message = "validation.reason.size255")
        String motivo
) {
}
