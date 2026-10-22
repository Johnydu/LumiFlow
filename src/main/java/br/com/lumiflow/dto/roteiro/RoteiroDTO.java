package br.com.lumiflow.dto.roteiro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;


import java.util.List;

public record RoteiroDTO(

        Long id,

        @NotBlank(message = "validation.code.required")
        @Size(max = 30, message = "validation.code.size30")
        String codigo,

        @Size(max = 255, message = "validation.description.size255")
        String descricao,

        @NotEmpty(message = "validation.route.steps.required")
        @Valid
        List<PassoRoteiroDTO> passos
) {
}
