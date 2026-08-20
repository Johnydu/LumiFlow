package br.com.lumiflow.dto.vidracaria;

import br.com.lumiflow.model.enums.TipoMovimentacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record MovimentacaoVidroDTO(
        @NotNull(message = "Selecione o tipo de vidro")
        Long chapaVidroId,

        @NotNull
        TipoMovimentacao tipoMovimentacao, // ENTRADA ou CONSUMO

        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "Selecione um operador")
        Long operadorId,

        LocalDateTime dataHora,

        String observacao
) {}
