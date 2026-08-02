package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.nivelacesso.NivelAcessoDTO;
import br.com.lumiflow.model.NivelAcesso;
import org.mapstruct.Mapper;

import java.util.List;


@Mapper(componentModel = "spring")
public interface NivelAcessoMapper {


    List<NivelAcessoDTO> toListDTO(List<NivelAcesso>  nivelAcessoList );
}
