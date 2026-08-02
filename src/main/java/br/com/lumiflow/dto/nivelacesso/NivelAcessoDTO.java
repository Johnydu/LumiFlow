package br.com.lumiflow.dto.nivelacesso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NivelAcessoDTO(

        @NotNull
        Long id,

        @NotBlank
        String descricao
) {
}
