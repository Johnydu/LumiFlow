package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.model.Operador;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OperadorMapper {

    // 1. DTO -> Entidade (Cadastro e Edição)
    @Mapping(target = "nome", source = "nome", qualifiedByName = "normalizarNome")
    @Mapping(target = "funcao", source = "funcao", qualifiedByName = "normalizarNome")
    @Mapping(target = "setorPadrao", ignore = true) // A busca da entidade Setor é feita no Service pelo ID
    Operador toEntity(OperadorDTO dto);

    // 2. Entidade -> DTO (Para carregar dados de edição, se necessário)
    @Mapping(target = "nome", source = "nome", qualifiedByName = "normalizarNome")
    @Mapping(target = "setorPadraoId", source = "setorPadrao.id")
    OperadorDTO toDto(Operador operador);

    // 3. Entidade -> Listagem DTO (Para a Tabela)
    @Mapping(target = "setorPadraoId", source = "setorPadrao.id")
    @Mapping(target = "setorPadraoNome", source = "setorPadrao.nome")
    OperadorListagemDTO toListagemDto(Operador operador);

    // Mapeamento da lista de exibição
    List<OperadorListagemDTO> toListDto(List<Operador> operadores);

    @Named("normalizarNome")
    default String normalizarNome(String nome) {
        return nome == null ? null : nome.trim().toUpperCase(java.util.Locale.ROOT);
    }
}