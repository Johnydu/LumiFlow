package br.com.lumiflow.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioDTO(

        Long id,

        @NotBlank
        @Size(max = 50)
        String nome,

        @NotBlank
        @Size(max = 20)
        String login,

        @NotBlank
        @Size(min = 8, max = 72, message = "validation.password.size")
        String senha,

        @NotNull
        Long nivelAcessoId,

        Long setorId

) {
        public UsuarioDTO semSenha() {
                return new UsuarioDTO(id, nome, login, null, nivelAcessoId, setorId);
        }
}
