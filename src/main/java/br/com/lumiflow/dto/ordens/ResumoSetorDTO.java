package br.com.lumiflow.dto.ordens;

import java.time.LocalDate;

public record ResumoSetorDTO(
        Long setorId,
        String setorNome,
        LocalDate data,
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