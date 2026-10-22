package br.com.lumiflow.dto.roteiro;

public record PassoDoRoteiroDTO(
        Long roteiroId,
        Integer sequencia,
        String etapaNome
) {
}
