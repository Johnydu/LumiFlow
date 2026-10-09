package br.com.lumiflow.dto.vidracaria;

import br.com.lumiflow.entity.enums.TipoMovimentacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record MovimentacaoVidroDTO(
        @NotNull(message = "Selecione o tipo de vidro")
        Long chapaVidroId,

        @NotNull
        TipoMovimentacao tipoMovimentacao,

        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "Selecione um operador")
        Long operadorId,

        LocalDateTime dataHora,

        @Size(max = 255)
        String observacao
) {}
