package br.com.lumiflow.dto.setor;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;

import java.util.List;

public record SetorListagemDTO(
        Long id,
        String nome,

        Integer quantidadeMaquinas,

        List<EtapaSetorDTO> etapas


) {
}
