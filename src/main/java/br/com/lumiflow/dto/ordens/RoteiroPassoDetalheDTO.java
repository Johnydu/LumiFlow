package br.com.lumiflow.dto.ordens;

import br.com.lumiflow.entity.enums.SituacaoPasso;

public record RoteiroPassoDetalheDTO(
        String rotulo,
        SituacaoPasso situacao
) {
}