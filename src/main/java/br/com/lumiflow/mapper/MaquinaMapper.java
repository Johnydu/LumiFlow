package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.model.Maquina;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MaquinaMapper {

    @Mapping(target = "setor", ignore = true)
    Maquina toEntity(MaquinaDTO maquinaDTO);


    @Mapping(target = "setor", source = "setor.nome")
    @Mapping(target = "setorId", source = "setor.id")
    MaquinaDTO toDto(Maquina maquina);

    List<MaquinaDTO> toDtoList(List<Maquina> maquinas);
}
