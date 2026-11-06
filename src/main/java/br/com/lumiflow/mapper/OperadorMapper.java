package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.entity.Operador;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OperadorMapper {

    // 1. DTO -> Entidade (Cadastro). O id nunca vem do DTO: um POST com id faria merge/sobrescreveria outro registro.
    @Mapping(target = "nome", source = "nome")
    @Mapping(target = "funcao", source = "funcao")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "setorPadrao", ignore = true) // A busca da entidade Setor é feita no Service pelo ID
    Operador toEntity(OperadorDTO dto);



    // 3. Entidade -> Listagem DTO (Para a Tabela)
    @Mapping(target = "setorPadraoId", source = "setorPadrao.id")
    @Mapping(target = "setorPadraoNome", source = "setorPadrao.nome")
    OperadorListagemDTO toListagemDto(Operador operador);

    // Mapeamento da lista de exibição
    List<OperadorListagemDTO> toListDto(List<Operador> operadores);


}