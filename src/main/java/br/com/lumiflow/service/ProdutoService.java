package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.produto.ProdutoListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.ProdutoMapper;
import br.com.lumiflow.entity.Produto;
import br.com.lumiflow.repository.ProdutoRepository;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdutoService {
    private final ProdutoRepository produtoRepository;
    private final ProdutoMapper produtoMapper;

    private void validarNomeProduto(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
        if (produtoRepository.existsByNome(nome.trim())) {
            throw new BusinessException(AppMessages.ERROR_PRODUCT_NOME_DUPLICATE);
        }
    }

    private void validarCodigoProduto(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
        if (produtoRepository.existsByCodigo(codigo.trim())) {
            throw new BusinessException(AppMessages.ERROR_PRODUCT_CODIGO_DUPLICATE);
        }
    }

    @Transactional(readOnly = true)
    public List<ProdutoListagemDTO> listarProdutos() {
        try {
            return produtoMapper.toListagemDTO(produtoRepository.findAllByOrderByNomeAsc());
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar produtos: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar produtos: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void novoProduto(ProdutoDTO dto) {
        try {
            if (dto == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            validarNomeProduto(dto.nome());
            validarCodigoProduto(dto.codigo());

            Produto produto = produtoMapper.toEntity(dto);
            produto.setNome(dto.nome().trim());
            if (dto.codigo() != null) {
                produto.setCodigo(dto.codigo().trim());
            }

            produtoRepository.save(produto);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao criar novo produto: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar novo produto: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    public Produto buscarProdutoPorId(@NotNull Long id) {
        try {
            if (id == null) {
                throw new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND);
            }
            return produtoRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND));
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar produto id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar produto id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND);
        }
    }

    @Transactional
    public void deletarProduto(Long id) {
        try {
            Produto produto = buscarProdutoPorId(id);
            produtoRepository.delete(produto);
        } catch (BusinessException e) {
            throw e;
        } catch (DataIntegrityViolationException ex) {
            log.error("Violação de integridade ao excluir produto id {}: {}", id, ex.getMessage(), ex);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao excluir produto id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir produto id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void editarProduto(Long id, ProdutoDTO dto) {
        try {
            if (dto == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            Produto produto = buscarProdutoPorId(id);

            if (dto.nome() != null && !dto.nome().isBlank()) {
                String novoNomeTrim = dto.nome().trim();
                if (!novoNomeTrim.equalsIgnoreCase(produto.getNome()) && produtoRepository.existsByNome(novoNomeTrim)) {
                    throw new BusinessException(AppMessages.ERROR_PRODUCT_NOME_DUPLICATE);
                }
                produto.setNome(novoNomeTrim);
            }

            if (dto.codigo() != null && !dto.codigo().isBlank()) {
                String novoCodigoTrim = dto.codigo().trim();
                if (!novoCodigoTrim.equalsIgnoreCase(produto.getCodigo()) && produtoRepository.existsByCodigo(novoCodigoTrim)) {
                    throw new BusinessException(AppMessages.ERROR_PRODUCT_CODIGO_DUPLICATE);
                }
                produto.setCodigo(novoCodigoTrim);
            }

            produto.setDescricao(dto.descricao());
            produtoRepository.save(produto);

        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao editar produto id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar produto id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<ProdutoListagemDTO> buscarProdutos(String termo) {
        try {
            if (termo == null || termo.isBlank()) {
                return listarProdutos();
            }
            String termoTrim = termo.trim();
            List<Produto> produtos = produtoRepository
                    .findByNomeContainingIgnoreCaseOrCodigoContainingIgnoreCase(termoTrim, termoTrim);
            return produtoMapper.toListagemDTO(produtos);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao pesquisar produtos por termo '{}': {}", termo, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao pesquisar produtos por termo '{}': {}", termo, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}