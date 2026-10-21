package br.com.lumiflow.dto.maquina;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MaquinaDTO(

        Long id,

        @NotBlank(message = "Informe o nome da máquina")
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        String nome,

        String setor,

        @NotNull(message = "Selecione um setor")
        Long setorId
) {
}
