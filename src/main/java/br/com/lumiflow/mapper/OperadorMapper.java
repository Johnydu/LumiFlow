package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.model.Operador;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OperadorMapper {

    Operador toEntity(OperadorDTO operadorDTO);

    List<OperadorListagemDTO> toListDto(List<Operador> operadores);


}
