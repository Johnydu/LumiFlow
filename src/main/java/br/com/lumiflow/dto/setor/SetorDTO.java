package br.com.lumiflow.dto.setor;

import br.com.lumiflow.entity.EtapaSetor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public record SetorDTO(

        Long id,

        @NotBlank
        String nome,

        @NotNull
        Boolean possuiEtapas

) {
}
