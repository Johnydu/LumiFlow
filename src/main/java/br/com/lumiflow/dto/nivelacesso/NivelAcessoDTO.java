package br.com.lumiflow.dto.nivelacesso;

import br.com.lumiflow.entity.enums.Descricao;

public record NivelAcessoDTO(
        Long id,
        Descricao descricao
) {
}
