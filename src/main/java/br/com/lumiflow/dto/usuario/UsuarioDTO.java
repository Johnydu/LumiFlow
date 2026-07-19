package br.com.lumiflow.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioDTO(

        @NotNull
        Long id,

        @NotBlank
        String nome,

        @NotBlank
        String login,

        @NotBlank
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String senha,

        @NotNull
        Long nivelAcessoId,


        Long setorId

) {}
