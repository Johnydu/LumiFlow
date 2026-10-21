package br.com.lumiflow.dto.produto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record ProdutoDTO(

        Long id,


        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @NotBlank(message = "Campo código obrigatorio !")
        @Size(max = 30, message = "O código deve ter no máximo 30 caracteres")
        String codigo,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao
) {
}
