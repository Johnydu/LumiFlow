package br.com.lumiflow.dto.ordemsetor;

import br.com.lumiflow.dto.operador.OperadorDTO;

import java.time.LocalDate;
import java.util.List;

public record OrdemsSetorListagemDTO(

        LocalDate data,

        String setorid,

        List<OperadorDTO> operadoresDia,

        Integer qtdOrdens


) {
}
