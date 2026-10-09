package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.roteiro.PassoRoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroListagemDTO;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.entity.RoteiroProduto;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.RoteiroProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoteiroService {
    private final RoteiroProdutoRepository roteiroProdutoRepository;
    private final ProdutoService produtoService;
    private final SetorService setorService;
    private final EtapaSetorService etapaSetorService;
    private final OrdemProducaoRepository ordemProducaoRepository;

    @Transactional(readOnly = true)
    public RoteiroDTO buscarRoteiroPorProdutoId(Long produtoId) {
        try {
            if (produtoId == null) {
                throw new BusinessException(AppMessages.ERROR_ROUTE_PRODUCT_REQUIRED);
            }
            List<RoteiroProduto> roteiros = roteiroProdutoRepository.buscarPorProdutoId(produtoId);
            return new RoteiroDTO(produtoId, roteiros.stream().map(r -> new PassoRoteiroDTO(r.getSetor()
                    .getId(), r.getEtapaSetor() == null ? null : r.getEtapaSetor()
                    .getId())).toList());
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar roteiro para o produto id {}: {}", produtoId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar roteiro para o produto id {}: {}", produtoId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<RoteiroListagemDTO> listarRoteiros() {
        try {
            return roteiroProdutoRepository.listarOrdenados().stream()
                    .collect(Collectors.groupingBy(r -> r.getProduto()
                            .getId(), java.util.LinkedHashMap::new, Collectors.toList()))
                    .values().stream().map(lista -> {
                        RoteiroProduto primeiro = lista.getFirst();

                        List<String> fluxo = lista.stream().map(r -> r.getEtapaSetor() == null ? r.getSetor()
                                .getNome() : r.getSetor().getNome() + " (" + r.getEtapaSetor().getNome() + ")").toList();
                        return new RoteiroListagemDTO(primeiro.getProduto().getId(), primeiro.getProduto()
                                .getNome(), primeiro.getProduto().getCodigo(), lista.size(), fluxo);

                    }).toList();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar roteiros: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar roteiros: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void criar(RoteiroDTO dto) {
        try {
            validarRoteiro(dto);
            if (!roteiroProdutoRepository.buscarPorProdutoId(dto.produtoId()).isEmpty())
                throw new BusinessException(AppMessages.ERROR_ROUTE_ALREADY_EXISTS);
            salvarPersistenciaRoteiro(dto.produtoId(), dto);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao criar roteiro para o produto id {}: {}", dto != null ? dto.produtoId() : null, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar roteiro: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void atualizar(Long produtoIdOriginal, RoteiroDTO dto) {
        try {
            if (produtoIdOriginal == null || dto == null) {
                throw new BusinessException(AppMessages.ERROR_ROUTE_PRODUCT_REQUIRED);
            }
            // Valida se o produto do DTO corresponde ao da URL
            if (!produtoIdOriginal.equals(dto.produtoId())) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            // Impede a atualização se existirem ordens em andamento/abertas para este produto
            if (ordemProducaoRepository.existsByProdutoIdAndStatusNot(produtoIdOriginal, EstatusOrdemProducao.CONCLUIDA)) {
                throw new BusinessException(AppMessages.ERROR_ROUTE_IN_USE);
            }
            validarRoteiro(dto);
            roteiroProdutoRepository.deletarPorProdutoId(produtoIdOriginal);
            salvarPersistenciaRoteiro(dto.produtoId(), dto);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao atualizar roteiro do produto id {}: {}", produtoIdOriginal, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar roteiro do produto id {}: {}", produtoIdOriginal, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void excluirPorProdutoId(Long produtoId) {
        try {
            if (produtoId == null) {
                throw new BusinessException(AppMessages.ERROR_ROUTE_PRODUCT_REQUIRED);
            }
            // Impede a exclusão se existirem ordens em andamento/abertas para este produto
            if (ordemProducaoRepository.existsByProdutoIdAndStatusNot(produtoId, EstatusOrdemProducao.CONCLUIDA)) {
                throw new BusinessException(AppMessages.ERROR_ROUTE_IN_USE);
            }
            roteiroProdutoRepository.deletarPorProdutoId(produtoId);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao excluir roteiro do produto id {}: {}", produtoId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir roteiro do produto id {}: {}", produtoId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    private void validarRoteiro(RoteiroDTO dto) {
        if (dto == null) {
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
        if (dto.produtoId() == null) throw new BusinessException(AppMessages.ERROR_ROUTE_PRODUCT_REQUIRED);
        if (dto.passos() == null || dto.passos().isEmpty())
            throw new BusinessException(AppMessages.ERROR_ROUTE_STEP_REQUIRED);
    }

    private void salvarPersistenciaRoteiro(Long produtoId, RoteiroDTO dto) {
        var produto = produtoService.buscarProdutoPorId(produtoId);
        List<RoteiroProduto> roteiro = new ArrayList<>();
        int sequencia = 10;
        for (var passo : dto.passos()) {
            if (passo == null || passo.setorId() == null) {
                throw new BusinessException(AppMessages.ERROR_ROUTE_STEP_REQUIRED);
            }
            var item = new RoteiroProduto();
            item.setProduto(produto);
            item.setSetor(setorService.buscarSetorPorId(passo.setorId()));
            item.setSequencia(sequencia);
            if (passo.etapaSetorId() != null) {
                var etapa = java.util.Optional.ofNullable(etapaSetorService.buscarPorId(passo.etapaSetorId()))
                        .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ROUTE_STEP_NOTFOUND,
                                new Object[]{passo.etapaSetorId()}));

                // Valida se a etapa pertence de facto ao setor indicado no passo
                if (!etapa.getSetor().getId().equals(passo.setorId())) {
                    throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
                }

                item.setEtapaSetor(etapa);
            }
            roteiro.add(item);
            sequencia += 10;
        }
        roteiroProdutoRepository.saveAll(roteiro);
    }

    @Transactional(readOnly = true)
    public List<String> buscarFluxoPorProdutoId(Long produtoId) {
        try {
            if (produtoId == null) {
                return List.of();
            }
            return roteiroProdutoRepository.buscarPorProdutoId(produtoId).stream()
                    .map(r -> r.getEtapaSetor() == null
                            ? r.getSetor().getNome()
                            : r.getSetor().getNome() + " (" + r.getEtapaSetor().getNome() + ")")
                    .toList();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar fluxo do produto id {}: {}", produtoId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar fluxo do produto id {}: {}", produtoId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<RoteiroListagemDTO> buscarPorNomeOuCodigoProduto(String termo) {
        try {
            if (termo == null || termo.isBlank()) {
                return listarRoteiros();
            }

            String termoLower = termo.trim().toLowerCase();

            return roteiroProdutoRepository.listarOrdenados().stream()
                    .filter(r -> {
                        String nomeProduto = r.getProduto().getNome() != null ?
                                r.getProduto().getNome().toLowerCase() : "";
                        String codigoProduto = r.getProduto().getCodigo() != null ?
                                r.getProduto().getCodigo().toLowerCase() : "";
                        return nomeProduto.contains(termoLower) || codigoProduto.contains(termoLower);
                    })
                    .collect(Collectors.groupingBy(r -> r.getProduto()
                            .getId(), java.util.LinkedHashMap::new, Collectors.toList()))
                    .values().stream().map(lista -> {
                        RoteiroProduto primeiro = lista.getFirst();
                        List<String> fluxo = lista.stream().map(r -> r.getEtapaSetor() == null ? r.getSetor()
                                .getNome() : r.getSetor().getNome() + " (" + r.getEtapaSetor().getNome() + ")").toList();
                        return new RoteiroListagemDTO(primeiro.getProduto().getId(), primeiro.getProduto()
                                .getNome(), primeiro.getProduto().getCodigo(), lista.size(), fluxo);
                    }).toList();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao pesquisar roteiros por termo '{}': {}", termo, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao pesquisar roteiros por termo '{}': {}", termo, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<EtapaSetorDTO> buscarEtapasDoSetorNoRoteiro(Long produtoId, Long setorId) {
        return roteiroProdutoRepository.buscarPorProdutoId(produtoId).stream()
                .filter(rp -> rp.getSetor().getId().equals(setorId) && rp.getEtapaSetor() != null)
                .map(rp -> new EtapaSetorDTO(
                        rp.getEtapaSetor().getId(),
                        rp.getEtapaSetor().getNome(),
                        rp.getEtapaSetor().getOrdem()))
                .toList();
    }
}