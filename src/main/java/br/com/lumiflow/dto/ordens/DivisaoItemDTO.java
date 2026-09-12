package br.com.lumiflow.dto.ordens;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DivisaoItemDTO(
        @NotBlank(message = "A variante é obrigatória")
        String variante,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        Integer quantidade
) {}