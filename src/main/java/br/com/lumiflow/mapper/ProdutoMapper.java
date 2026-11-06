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
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roteiro", ignore = true)
    Produto toEntity(ProdutoDTO dto);



    List<ProdutoDTO>  toListDTO(List<Produto> produtoList);

    @Mapping(target = "roteiroCodigo", source = "roteiro.codigo")
    @Mapping(target = "roteiroDescricao", source = "roteiro.descricao")
    ProdutoListagemDTO toListagemDTO(Produto produto);

    List<ProdutoListagemDTO> toListagemDTO(List<Produto> produtoListagem);

}
