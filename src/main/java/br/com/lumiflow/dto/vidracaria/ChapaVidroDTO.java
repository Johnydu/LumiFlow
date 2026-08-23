package br.com.lumiflow.dto.vidracaria;

import br.com.lumiflow.entity.enums.TipoVidro;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ChapaVidroDTO(
        @NotNull(message = "Selecione o tipo de vidro")
        TipoVidro tipoVidro,

        String descricao,

        @NotNull(message = "O estoque mínimo é obrigatório")
        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo")
        Integer estoqueMinimo
) {}