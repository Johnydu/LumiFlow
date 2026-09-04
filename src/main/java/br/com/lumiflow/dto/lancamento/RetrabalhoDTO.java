package br.com.lumiflow.dto.lancamento;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RetrabalhoDTO(
        @NotNull(message = "O refugo vinculado é obrigatório")
        Long refugoId,

        @NotNull(message = "A máquina é obrigatória")
        Long maquinaId,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        String observacao
) {}