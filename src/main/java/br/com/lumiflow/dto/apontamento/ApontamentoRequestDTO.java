package br.com.lumiflow.dto.apontamento;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Dados enviados pelo operador ao lançar um apontamento de produção.
 * Validado na borda (Controller) antes de chegar no Service.
 */
public record ApontamentoRequestDTO(

        @NotNull(message = "Informe a ordem/setor")
        Long ordemSetorId,

        @NotNull(message = "Informe a máquina utilizada")
        Long maquinaId,

        @NotNull(message = "Quantidade produzida é obrigatória")
        @Min(value = 0, message = "Quantidade produzida não pode ser negativa")
        Integer qtdProduzida,

        @Min(value = 0, message = "Quantidade de refugo não pode ser negativa")
        Integer qtdRefugo,

        String motivoRefugo
) {
}