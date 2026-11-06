package br.com.lumiflow.dto.vidracaria;

import br.com.lumiflow.entity.enums.TipoVidro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ChapaVidroDTO(
        @NotNull(message = "validation.glass.type.required")
        TipoVidro tipoVidro,

        @NotBlank(message = "validation.glass.description.required")
        @Size(max = 100, message = "validation.description.size100")
        String descricao,

        @NotNull(message = "validation.stock.min.required")
        @PositiveOrZero(message = "validation.stock.min.negative")
        Integer estoqueMinimo
) {}
