package br.com.lumiflow.dto.roteiro;

import java.util.List;

public record VincularProdutosDTO(
        List<Long> produtoIds
) {
}
