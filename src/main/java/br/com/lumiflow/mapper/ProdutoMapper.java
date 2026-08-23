package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.produto.ProdutoListagemDTO;
import br.com.lumiflow.entity.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    ProdutoDTO toDTO(Produto produto);

    @Mapping(target = "nome", source = "nome", qualifiedByName = "normalizarNome")
    Produto toEntity(ProdutoDTO dto);

    @Named("normalizarNome")
    default String normalizarNome(String nome) {
        return nome == null
                ? null
                : nome.trim().toUpperCase(java.util.Locale.ROOT);
    }

    List<ProdutoDTO>  toListDTO(List<Produto> produtoList);

    List<ProdutoListagemDTO>  toListagemDTO(List<Produto> produtoListagem);


}
