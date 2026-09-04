package br.com.lumiflow.dto.lancamento;

import br.com.lumiflow.entity.enums.DestinoRefugo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RefugoDTO(
        @NotNull(message = "A quantidade é obrigatória")
        @Min(value = 1, message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "O setor de origem é obrigatório")
        Long setorOrigemId,

        @NotNull(message = "O destino é obrigatório")
        DestinoRefugo destino,

        @NotBlank(message = "O motivo é obrigatório")
        String motivo
) {
}