package br.com.lumiflow.dto.ordens;


public record OrdemFiltroDTO(
        String setor,
        String busca,
        String status
) {
    public boolean temFiltroSetor() {
        return setor != null && !setor.isBlank();
    }
}