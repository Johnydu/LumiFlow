package br.com.lumiflow.dto.maquina;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaquinaDTO(

        Long id,

        @NotBlank(message = "Informe o nome da máquina")
        String nome,

        String setor,

        @NotNull(message = "Selecione um setor")
        Long setorId
) {
}
