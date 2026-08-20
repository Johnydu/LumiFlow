package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.model.ChapaVidro;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChapaVidroMapper {

    ChapaVidro toEntity(ChapaVidroDTO dto);
}
