package br.com.lumiflow.dto;


import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.Setor;

public record UsuarioDTO(
        Long id,
        String nome,
        String login,
        String senha,
        NivelAcesso nivelAcesso,
        Setor setor

) {}
