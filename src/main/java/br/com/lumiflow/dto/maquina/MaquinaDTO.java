package br.com.lumiflow.dto.maquina;

import br.com.lumiflow.entity.Setor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaquinaDTO(

        Long id,

        @NotBlank
        String nome,

        @NotNull
        Setor setor
) {
}
