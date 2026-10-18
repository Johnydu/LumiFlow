package br.com.lumiflow.dto.roteiro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;


import java.util.List;

public record RoteiroDTO(

        Long id,

        @NotBlank(message = "O código é obrigatório")
        @Size(max = 30, message = "O código deve ter no máximo 30 caracteres")
        String codigo,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao,

        @NotEmpty(message = "Adicione ao menos uma etapa ao roteiro")
        @Valid
        List<PassoRoteiroDTO> passos
) {
}