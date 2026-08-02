package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.produto.ProdutoListagemDTO;
import br.com.lumiflow.model.Produto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProdutoMapper {

    ProdutoDTO toDTO(Produto produto);

    Produto  toEntity(ProdutoDTO produtoDTO);

    List<ProdutoDTO>  toListDTO(List<Produto> produtoList);

    List<ProdutoListagemDTO>  toListagemDTO(List<Produto> produtoListagem);


}
