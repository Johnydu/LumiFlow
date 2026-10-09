package br.com.lumiflow.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioEdicaoDTO(

        @NotNull
        Long id,

        @NotBlank
        @Size(max = 50)
        String nome,

        @NotBlank
        @Size(max = 20)
        String login,

        @NotNull
        Long nivelAcessoId,


        String senha,

        Long setorId
) {
        public UsuarioDTO semSenha() {
                return new UsuarioDTO(id, nome, login, null, nivelAcessoId, setorId);
        }
}
