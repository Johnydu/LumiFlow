package br.com.lumiflow.dto.produto;

import jakarta.validation.constraints.NotBlank;

public record ProdutoDTO(

        Long id,


        String nome,

        @NotBlank(message = "Campo código obrigatorio !")
        String codigo,

        String descricao
) {
}
