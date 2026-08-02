package br.com.lumiflow.service;

import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.produto.ProdutoListagemDTO;
import br.com.lumiflow.model.Produto;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.ProdutoMapper;
import br.com.lumiflow.repository.ProdutoRepository;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ProdutoMapper produtoMapper;

    public void validarProduto(String nome){

        if(produtoRepository.findByNome(nome).isPresent()){
            throw new BusinessException("Produto já cadastrado");
        }
    }

    public Produto buscarPorNome(String nome) {
        return produtoRepository.findByNome(nome).orElseThrow(()-> new BusinessException("Produto não encontrado"));
    }

    public List<ProdutoListagemDTO> listarProdutosPorCodigo(){

        return produtoMapper.toListagemDTO(produtoRepository.findAllByOrderByNomeAsc());
    }

    public void novoProduto(ProdutoDTO produtoDTO){

        validarProduto(produtoDTO.nome());

        produtoRepository.save(produtoMapper.toEntity(produtoDTO));

    }

    public Produto buscarProdutoPorId(@NotNull Long id) {
        return produtoRepository.findById(id).orElseThrow(()-> new BusinessException("Produto não encontrado"));
    }

    public void deletarProduto(Long id) {

        buscarProdutoPorId(id);
        produtoRepository.deleteById(id);

    }

    public void editarProduto(Long id, ProdutoDTO produtoDTO) {
        Produto produto = buscarProdutoPorId(id);

        produto.setNome(produtoDTO.nome());
        produto.setCodigo(produtoDTO.codigo());
        produto.setDescricao(produtoDTO.descricao());

        produtoRepository.save(produto);




    }
}
