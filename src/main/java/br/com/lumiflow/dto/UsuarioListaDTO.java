package br.com.lumiflow.dto;

import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.Setor;

public record UsuarioListaDTO(

        Long id,
        String nome,
        String login,
        NivelAcesso nivelAcesso,
        Setor setor
) {
}
