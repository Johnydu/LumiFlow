package br.com.lumiflow.dto.roteiro;

public record ProdutoVinculoDTO(
        Long id,
        String nome,
        String codigo,
        String roteiroCodigo
) {
}
