package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.produto.ProdutoListagemDTO;
import br.com.lumiflow.entity.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    ProdutoDTO toDTO(Produto produto);

    @Mapping(target = "nome", source = "nome")
    @Mapping(target = "roteiro", ignore = true)
    Produto toEntity(ProdutoDTO dto);



    List<ProdutoDTO>  toListDTO(List<Produto> produtoList);

    List<ProdutoListagemDTO>  toListagemDTO(List<Produto> produtoListagem);


}
