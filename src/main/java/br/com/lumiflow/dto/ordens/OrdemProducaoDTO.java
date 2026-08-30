package br.com.lumiflow.dto.ordens;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OrdemProducaoDTO(

        @NotNull(message = "O produto é obrigatório")
        Long produtoId,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "A data é obrigatória")
        LocalDate dataCriacao,

        String observacao
) {
}