package br.com.lumiflow.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioListaDTO(

        @NotNull
        Long id,

        @NotBlank
        String nome,

        @NotBlank
        String login,

        @NotNull
        Long nivelAcessoId,

        @NotNull
        Long setorId,

        @NotBlank
        String nivelAcesso,

        @NotBlank
        String setor
) {
}
