package br.com.lumiflow.dto.vidracaria;

import br.com.lumiflow.entity.enums.TipoMovimentacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record MovimentacaoVidroDTO(
        @NotNull(message = "validation.glass.type.required")
        Long chapaVidroId,

        @NotNull(message = "validation.glass.movement.type.required")
        TipoMovimentacao tipoMovimentacao,

        @NotNull(message = "validation.quantity.required")
        @Positive(message = "validation.quantity.positive")
        Integer quantidade,

        @NotNull(message = "validation.operator.select")
        Long operadorId,

        LocalDateTime dataHora,

        @Size(max = 255, message = "validation.notes.size255")
        String observacao
) {}
