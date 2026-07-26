package br.com.lumiflow.dto.setor;

public record SetorListagemDTO(
        Long id,

        String nome,

        Boolean possuiEtapas,

        Integer quantidadeMaquinas


) {
}
