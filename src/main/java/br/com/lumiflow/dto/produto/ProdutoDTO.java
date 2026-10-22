package br.com.lumiflow.dto.produto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record ProdutoDTO(

        Long id,


        @Size(max = 100, message = "validation.name.size100")
        String nome,

        @NotBlank(message = "validation.code.required")
        @Size(max = 30, message = "validation.code.size30")
        String codigo,

        @Size(max = 255, message = "validation.description.size255")
        String descricao
) {
}
