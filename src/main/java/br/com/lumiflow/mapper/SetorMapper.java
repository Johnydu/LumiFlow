package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.entity.Setor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;


@Mapper(componentModel = "spring")
public interface SetorMapper {

    @Mapping(target = "nome", source = "nome", qualifiedByName = "normalizarNome")
    Setor toEntity(SetorDTO dto);

    @Named("normalizarNome")
    default String normalizarNome(String nome) {
        return nome == null
                ? null
                : nome.trim().toUpperCase(java.util.Locale.ROOT);
    }

    SetorDTO toDto(Setor setor);



}
