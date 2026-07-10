package br.com.lumiflow.dto;

import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.Setor;

public record UsuarioListaDTO(

        String nome,
        String login,
        NivelAcesso nivelAcesso,
        Setor setor
) {
}
