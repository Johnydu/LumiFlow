package br.com.lumiflow.dto.lancamento;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RetrabalhoDTO(
        @NotNull(message = "O refugo vinculado é obrigatório")
        Long refugoId,


        Long maquinaId,

        @NotNull(message = "O operador é obrigatório")
        Long operadorId,

        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres")
        String observacao
) {}