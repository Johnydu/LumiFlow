package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.entity.Maquina;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MaquinaMapper {

    @Mapping(target = "setor", ignore = true)
    @Mapping(target = "nome", source = "nome", qualifiedByName = "normalizarNome")
    Maquina toEntity(MaquinaDTO maquinaDTO);

    @Named("normalizarNome")
    default String normalizarNome(String nome) {
        return nome == null
                ? null
                : nome.trim().toUpperCase(java.util.Locale.ROOT);
    }


    @Mapping(target = "setor", source = "setor.nome")
    @Mapping(target = "setorId", source = "setor.id")
    MaquinaDTO toDto(Maquina maquina);

    List<MaquinaDTO> toDtoList(List<Maquina> maquinas);
}
