package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.entity.Setor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SetorMapper {

    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    Setor toEntity(SetorDTO dto);

    SetorDTO toDto(Setor setor);

    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    List<SetorDTO> toListDto(List<Setor> setorList);

    List<Setor> toListEntity(List<SetorDTO> setorDTOList);

}
