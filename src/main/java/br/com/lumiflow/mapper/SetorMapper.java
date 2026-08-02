package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.model.Setor;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface SetorMapper {

   
    Setor toEntity(SetorDTO dto);

    SetorDTO toDto(Setor setor);



}
