package br.com.lumiflow.dto.usuario;


import jakarta.validation.constraints.NotBlank;

public record UsuarioLogadoDTO(

        @NotBlank
        String nome,

        @NotBlank
        String nivelAcesso
) {
}
