package br.com.lumiflow.dto.roteiro;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record RoteiroDTO(

        @NotNull
        Long produtoId,

        @NotEmpty
        List<PassoRoteiroDTO> passos

) {
}