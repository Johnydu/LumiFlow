package br.com.lumiflow.dto.usuario;

import jakarta.validation.constraints.Max;
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
        @Max(value = 72, message = "A senha deve ter no máximo 72 caracteres")
        @Size(min = 8, message = "A senha deve ter no mínimo 6 caracteres")
        String senha,

        @NotNull
        Long nivelAcessoId,


        Long setorId

) {
        public UsuarioDTO semSenha() {
                return new UsuarioDTO(id, nome, login, null, nivelAcessoId, setorId);
        }
}
