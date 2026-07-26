package br.com.lumiflow.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioEdicaoDTO(

        @NotNull
        Long id,

        @NotBlank
        String nome,

        @NotBlank
        String login,

        @NotNull
        Long nivelAcessoId,


        String senha,

        Long setorId
) {
}
