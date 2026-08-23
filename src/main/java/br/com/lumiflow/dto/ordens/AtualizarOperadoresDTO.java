package br.com.lumiflow.dto.ordens;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AtualizarOperadoresDTO(
        @NotNull(message = "O ID do setor é obrigatório.")
        Long setorId,

        @NotNull(message = "A data de alocação é obrigatória.")
        LocalDate data,

        @Size(max = 500, message = "O nome dos operadores não pode exceder 500 caracteres.")
        String operadores
) {}