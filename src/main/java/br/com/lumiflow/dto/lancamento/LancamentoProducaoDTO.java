package br.com.lumiflow.dto.lancamento;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LancamentoProducaoDTO(
        @NotNull(message = "A máquina é obrigatória")
        Long maquinaId,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        String observacao
) {}