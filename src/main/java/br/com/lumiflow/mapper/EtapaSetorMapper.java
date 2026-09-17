package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.entity.EtapaSetor;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EtapaSetorMapper {


    List<EtapaSetorDTO> toListDTO(List<EtapaSetor> bySetorIdOrderByOrdemAsc);

}
