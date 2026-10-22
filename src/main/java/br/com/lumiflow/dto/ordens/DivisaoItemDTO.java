package br.com.lumiflow.dto.ordens;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DivisaoItemDTO(
        @NotBlank(message = "validation.variant.required")
        String variante,

        @NotNull(message = "validation.quantity.required")
        @Min(value = 1, message = "validation.quantity.positive")
        Integer quantidade
) {}
