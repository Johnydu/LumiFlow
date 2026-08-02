package br.com.lumiflow.dto.roteiro;

import jakarta.validation.constraints.NotNull;

public record PassoRoteiroDTO(

        @NotNull
        Long setorId,

        Long etapaSetorId // Opcional, caso o setor tenha subdivisões (ex: 1ª e 2ª dobra)
) {}