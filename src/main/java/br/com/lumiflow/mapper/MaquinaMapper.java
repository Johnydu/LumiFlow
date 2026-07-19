package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.entity.Maquina;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MaquinaMapper {

    Maquina toEntity(MaquinaDTO maquinaDTO);
    MaquinaDTO toDto(Maquina maquina);
    List<Maquina> toListEntity(List<MaquinaDTO> maquinasDTO);
    List<MaquinaDTO> toDtoList(List<Maquina> maquinas);
}
