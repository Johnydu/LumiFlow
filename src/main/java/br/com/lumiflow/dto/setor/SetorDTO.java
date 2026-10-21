package br.com.lumiflow.dto.setor;

import jakarta.validation.constraints.Size;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;


public record SetorDTO(

        Long id,

        @NotBlank
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        String nome,


        List<EtapaSetorDTO> etapas

) {
}
