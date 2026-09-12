package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.ordens.OrdemProducaoDTO;
import br.com.lumiflow.dto.ordens.OrdenListaDTO;
import br.com.lumiflow.entity.OrdemProducao;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrdemProducaoMapper {

    OrdemProducao toEntity (OrdenListaDTO ordenListaDTO);

    OrdemProducaoDTO toDTO(OrdemProducao ordemProducao);



    List<OrdenListaDTO> toListDTO (List<OrdemProducao> ordensProducao);
}
