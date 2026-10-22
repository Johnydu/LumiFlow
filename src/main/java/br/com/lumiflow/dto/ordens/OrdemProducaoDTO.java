package br.com.lumiflow.dto.ordens;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OrdemProducaoDTO(

        @NotNull(message = "validation.product.required")
        Long produtoId,

        @NotNull(message = "validation.quantity.required")
        @Min(value = 1, message = "validation.quantity.positive")
        Integer quantidade,

        @NotNull(message = "validation.date.required")
        LocalDate dataCriacao,

        @Size(max = 500, message = "validation.notes.size500")
        String observacao
) {
}
