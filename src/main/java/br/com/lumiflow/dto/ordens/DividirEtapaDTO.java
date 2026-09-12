package br.com.lumiflow.dto.ordens;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DividirEtapaDTO(
        @NotEmpty(message = "Informe ao menos uma divisão")
        @Valid
        List<DivisaoItemDTO> divisoes
) {}