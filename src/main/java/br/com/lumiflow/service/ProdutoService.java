package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.produto.ProdutoListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.ProdutoMapper;
import br.com.lumiflow.model.Produto;
import br.com.lumiflow.repository.ProdutoRepository;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Locale;

@Service @AllArgsConstructor
public class ProdutoService {
    private final ProdutoRepository produtoRepository;
    private final ProdutoMapper produtoMapper;
    public void validarNomeProduto(String nome) { if (produtoRepository.existsByNome(nome)) throw new BusinessException(AppMessages.ERROR_PRODUCT_NOME_DUPLICATE); }
    public void validarCodigoProduto(String codigo) { if (produtoRepository.existsByCodigo(codigo)) throw new BusinessException(AppMessages.ERROR_PRODUCT_CODIGO_DUPLICATE); }
    public Produto buscarPorNome(String nome) { return produtoRepository.findByNome(nome).orElseThrow(() -> new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND)); }
    public List<ProdutoListagemDTO> listarProdutosPorCodigo() { return produtoMapper.toListagemDTO(produtoRepository.findAllByOrderByNomeAsc()); }
    public void novoProduto(ProdutoDTO dto) { validarNomeProduto(dto.nome()); validarCodigoProduto(dto.codigo()); produtoRepository.save(produtoMapper.toEntity(dto)); }
    public Produto buscarProdutoPorId(@NotNull Long id) { return produtoRepository.findById(id).orElseThrow(() -> new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND)); }
    public void deletarProduto(Long id) { produtoRepository.delete(buscarProdutoPorId(id)); }
    public void editarProduto(Long id, ProdutoDTO dto) { Produto produto = buscarProdutoPorId(id); produto.setNome(dto.nome().toUpperCase(Locale.ROOT)); produto.setCodigo(dto.codigo()); produto.setDescricao(dto.descricao()); produtoRepository.save(produto); }
}
