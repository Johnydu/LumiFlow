package br.com.lumiflow.dto.vidracaria;

import br.com.lumiflow.entity.enums.TipoVidro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ChapaVidroDTO(
        @NotNull(message = "Selecione o tipo de vidro")
        TipoVidro tipoVidro,

        @NotBlank
        @Size(max = 100)
        String descricao,

        @NotNull(message = "O estoque mínimo é obrigatório")
        @PositiveOrZero(message = "O estoque mínimo não pode ser negativo")
        Integer estoqueMinimo
) {}