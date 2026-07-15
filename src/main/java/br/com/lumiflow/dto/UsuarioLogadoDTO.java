package br.com.lumiflow.dto;

import br.com.lumiflow.entity.NivelAcesso;

public record UsuarioLogadoDTO(
        String nome,
        String nivelAcesso
) {
}
