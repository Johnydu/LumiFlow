package br.com.lumiflow.dto.etapaSetor;

import br.com.lumiflow.dto.ordens.DivisaoItemDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DividirEtapaDTO(
        @NotEmpty(message = "validation.split.required")
        @Valid
        List<DivisaoItemDTO> divisoes
) {}
