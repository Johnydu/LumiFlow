package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.entity.Setor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SetorMapper {

    @Mapping(target = "nome", source = "nome")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "etapas", ignore = true)
    Setor toEntity(SetorDTO dto);





    @Mapping(target = "quantidadeMaquinas", expression = "java(0)")
    @Mapping(target = "etapas", expression = "java(toEtapaDtoList(setor.getEtapas()))")
    SetorListagemDTO toListDTO(Setor setor);

    List<SetorListagemDTO> toListDTO(List<Setor> setores);

    default List<EtapaSetorDTO> toEtapaDtoList(List<EtapaSetor> etapas) {
        if (etapas == null) {
            return Collections.emptyList();
        }
        return etapas.stream()
                .map(e -> new EtapaSetorDTO(e.getId(), e.getNome(), e.getOrdem()))
                .toList();
    }
}