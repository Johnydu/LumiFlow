package br.com.lumiflow.dto.ordens;

public record ResumoSetorDTO(
        Long setorId,
        String setorNome,
        String operadores,
        Long qtdOrdens,
        Long ordensConcluidas
) {
    public Double percentualConcluido() {
        if (qtdOrdens == null || qtdOrdens == 0) {
            return 0.0;
        }
        return ((double) ordensConcluidas / qtdOrdens) * 100.0;
    }
}
