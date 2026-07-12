package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.NivelAcessoDTO;
import br.com.lumiflow.entity.NivelAcesso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NiveAcessoMapper {

    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    NivelAcesso toEntity(NivelAcessoDTO  nivelAcessoDTO);

    NivelAcessoDTO toDTO(NivelAcesso entity);

    List<NivelAcessoDTO> toListDTO(List<NivelAcesso> nivelAcessoList);

    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    List<NivelAcesso> toListEntity(List<NivelAcessoDTO> nivelAcessoDTOList);

}
