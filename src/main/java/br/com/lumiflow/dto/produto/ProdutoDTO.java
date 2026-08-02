package br.com.lumiflow.dto.produto;

import jakarta.validation.constraints.NotBlank;

public record ProdutoDTO(

        Long id,

        @NotBlank
        String nome,

        @NotBlank
        String codigo,

        String descricao
) {
}
