package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.DividirEtapaDTO;
import br.com.lumiflow.dto.ordens.*;
import br.com.lumiflow.dto.roteiro.PassoDoRoteiroDTO;
import br.com.lumiflow.entity.*;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import br.com.lumiflow.repository.RoteiroPassoRepository;
import br.com.lumiflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdemProducaoService {

    private static final int TAMANHO_PAGINA_ORDENS = 50;
    private static final List<EstatusOrdemProducao> STATUS_EM_ABERTO = List.of(
            EstatusOrdemProducao.DISPONIVEL,
            EstatusOrdemProducao.LIBERADA,
            EstatusOrdemProducao.EM_ANDAMENTO);

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final ProdutoService produtoService;
    private final RoteiroPassoRepository roteiroPassoRepository;
    private final OrdemSetorRepository ordemSetorRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Page<OrdemListagemDTO> listarPorSetorIdEFiltros(Long setorId, OrdemFiltroDTO filtro, int pagina) {
        try {
            if (setorId == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }

            String busca = filtro != null && filtro.busca() != null ? filtro.busca().trim() : "";
            String status = filtro != null ? filtro.status() : null;
            Pageable pageable = PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA_ORDENS);
            Page<OrdemSetor> etapas = ordemSetorRepository.buscarPorSetorIdEFiltros(
                    setorId, busca, statusDoFiltro(status), pageable);

            Set<Long> roteiroIds = etapas.getContent().stream()
                    .map(os -> os.getOrdemProducao().getRoteiro())
                    .filter(Objects::nonNull)
                    .map(Roteiro::getId)
                    .collect(Collectors.toSet());

            Map<String, String> etapaNomePorRoteiroESequencia = roteiroIds.isEmpty() ? Map.of() : roteiroPassoRepository
                    .buscarPassosPorRoteiroIds(roteiroIds).stream()
                    .filter(pp -> pp.etapaNome() != null)
                    .collect(Collectors.toMap(
                            pp -> pp.roteiroId() + "-" + pp.sequencia(),
                            PassoDoRoteiroDTO::etapaNome,
                            (x, y) -> x));

            return etapas.map(os -> montarOrdemListagemDTO(os, etapaNomePorRoteiroESequencia));
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar ordens por setor id {} e filtros: {}", setorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar ordens por setor id {} e filtros: {}", setorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    /** "" = em aberto (padrão); "TODAS" inclui concluídas; senão filtra pelo status. */
    private List<EstatusOrdemProducao> statusDoFiltro(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_EM_ABERTO;
        }
        if ("TODAS".equalsIgnoreCase(status.trim())) {
            return List.of(EstatusOrdemProducao.values());
        }
        try {
            return List.of(EstatusOrdemProducao.valueOf(status.trim()));
        } catch (IllegalArgumentException e) {
            return STATUS_EM_ABERTO;
        }
    }

    private OrdemListagemDTO montarOrdemListagemDTO(OrdemSetor ordemSetor, Map<String, String> etapaNomePorRoteiroESequencia) {
        OrdemProducao ordem = ordemSetor.getOrdemProducao();
        Integer totalEtapa = ordemSetor.getQtdRecebida();
        Integer produzido = ordemSetor.getQtdProduzida();
        Double percentual = totalEtapa == 0 ? 0.0 : (produzido * 100.0) / totalEtapa;

        String etapaNome = ordem.getRoteiro() == null ? null
                : etapaNomePorRoteiroESequencia.get(ordem.getRoteiro().getId() + "-" + ordemSetor.getSequencia());

        return new OrdemListagemDTO(
                ordem.getId(),
                ordemSetor.getId(),
                ordem.getNumero(),
                ordem.getProduto().getNome(),
                ordem.getProduto().getCodigo(),
                etapaNome,
                ordemSetor.getVariante(),
                ordemSetor.isOrigemRefugo(),
                totalEtapa,
                produzido,
                percentual,
                ordem.getObservacao(),
                ordem.getCriadoEm(),
                ordemSetor.getStatus()
        );
    }

    @Transactional
    public void criar(OrdemProducaoDTO dto, Long usuarioId) {
        try {
            if (dto == null || usuarioId == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            var produto = produtoService.buscarProdutoPorId(dto.produtoId());
            Roteiro roteiro = produto.getRoteiro();
            if (roteiro == null) {
                throw new BusinessException(AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE);
            }

            OrdemProducao ordem = new OrdemProducao();
            ordem.setNumero(gerarNumero());
            ordem.setQuantidade(dto.quantidade());
            ordem.setStatus(EstatusOrdemProducao.DISPONIVEL);
            ordem.setProduto(produto);
            ordem.setRoteiro(roteiro); // a ordem guarda o roteiro com que nasceu
            ordem.setCriadoPor(usuarioRepository.getReferenceById(usuarioId));
            ordem.setDataCriacao(dto.dataCriacao() != null ? dto.dataCriacao().atStartOfDay() : java.time.LocalDateTime.now());
            ordem.setObservacao(dto.observacao());

            ordemProducaoRepository.save(ordem);

            criarPrimeiraEtapaOrdemSetor(ordem);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao criar ordem de produção: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar ordem de produção: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    private void criarPrimeiraEtapaOrdemSetor(OrdemProducao ordem) {
        List<RoteiroPasso> roteiro = roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(ordem.getRoteiro().getId());

        if (roteiro.isEmpty()) {
            throw new BusinessException(AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE);
        }

        RoteiroPasso primeiroPasso = roteiro.getFirst(); // já vem ordenado por sequência

        OrdemSetor ordemSetor = new OrdemSetor();
        ordemSetor.setSequencia(primeiroPasso.getSequencia());
        ordemSetor.setQtdRecebida(ordem.getQuantidade());
        ordemSetor.setQtdProduzida(0);
        ordemSetor.setQtdPendente(ordem.getQuantidade());
        ordemSetor.setStatus(EstatusOrdemProducao.DISPONIVEL);
        ordemSetor.setOrdemProducao(ordem);
        ordemSetor.setSetor(primeiroPasso.getSetor());

        ordemSetorRepository.save(ordemSetor);
    }

    private String gerarNumero() {
        Long seq = ordemProducaoRepository.proximoNumeroSequencial();
        return "OP-" + String.format("%04d", seq != null ? seq : 1L);
    }

    @Transactional
    public void liberarOrdem(Long ordemSetorId) {
        try {
            if (ordemSetorId == null) {
                throw new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND);
            }
            OrdemSetor ordemSetor = ordemSetorRepository.findById(ordemSetorId)
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND));

            if (ordemSetor.getStatus() != EstatusOrdemProducao.DISPONIVEL) {
                throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
            }

            ordemSetor.setStatus(EstatusOrdemProducao.LIBERADA);
            ordemSetorRepository.save(ordemSetor);
        } catch (BusinessException e) {
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao liberar ordem setor id {}: {}", ordemSetorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao liberar ordem setor id {}: {}", ordemSetorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void pausarEtapa(Long ordemSetorId) {
        try {
            if (ordemSetorId == null) {
                throw new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND);
            }

            OrdemSetor ordemSetor = ordemSetorRepository.findById(ordemSetorId)
                    .orElseThrow(() ->
                            new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND));

            if (ordemSetor.getStatus() != EstatusOrdemProducao.LIBERADA
                    && ordemSetor.getStatus() != EstatusOrdemProducao.EM_ANDAMENTO) {
                throw new BusinessException(AppMessages.ERROR_ORDER_NOT_PAUSABLE);
            }

            ordemSetor.setStatus(EstatusOrdemProducao.DISPONIVEL);
            ordemSetorRepository.save(ordemSetor);

        } catch (BusinessException e) {
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao pausar etapa {}: {}",
                    ordemSetorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao pausar etapa {}: {}",
                    ordemSetorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void dividirEtapa(Long ordemSetorId, DividirEtapaDTO dto) {
        try {
            if (ordemSetorId == null || dto == null || dto.divisoes() == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }

            OrdemSetor ordemOriginal = ordemSetorRepository.findById(ordemSetorId)
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND));

            if (ordemOriginal.getStatus() != EstatusOrdemProducao.DISPONIVEL) {
                throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
            }

            // Linha de refugo não pode ser dividida: o retrabalho está vinculado a ela
            if (ordemOriginal.isOrigemRefugo()) {
                throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
            }

            // Etapa que já teve produção (por exemplo, pausada pelo PCP) não pode ser dividida
            if (ordemOriginal.getQtdProduzida() > 0) {
                throw new BusinessException(AppMessages.ERROR_DIVISION_ALREADY_STARTED);
            }

            int somaDivisoes = dto.divisoes().stream().mapToInt(DivisaoItemDTO::quantidade).sum();

            if (somaDivisoes > ordemOriginal.getQtdPendente()) {
                throw new BusinessException(AppMessages.ERROR_DIVISION_SUM_MISMATCH);
            }

            OrdemProducao ordem = ordemOriginal.getOrdemProducao();
            Setor setor = ordemOriginal.getSetor();
            Integer sequencia = ordemOriginal.getSequencia();

            Set<String> variantes = new HashSet<>();
            for (DivisaoItemDTO item : dto.divisoes()) {
                String variante = item.variante().trim().toUpperCase(Locale.ROOT);
                boolean repetidaNaDivisao = !variantes.add(variante);
                boolean jaExisteNaOrdem = ordemSetorRepository
                        .findByOrdemProducaoIdAndSequenciaAndVariante(
                                ordemOriginal.getOrdemProducao().getId(), ordemOriginal.getSequencia(), variante)
                        .isPresent();
                if (repetidaNaDivisao || jaExisteNaOrdem) {
                    throw new BusinessException(AppMessages.ERROR_DIVISION_VARIANT_DUPLICATE);
                }
            }

            for (DivisaoItemDTO item : dto.divisoes()) {
                OrdemSetor novaEtapa = new OrdemSetor();
                novaEtapa.setOrdemProducao(ordem);
                novaEtapa.setSetor(setor);
                novaEtapa.setSequencia(sequencia);
                novaEtapa.setVariante(item.variante().trim().toUpperCase(Locale.ROOT));
                novaEtapa.setQtdRecebida(item.quantidade());
                novaEtapa.setQtdProduzida(0);
                novaEtapa.setQtdPendente(item.quantidade());
                novaEtapa.setStatus(EstatusOrdemProducao.DISPONIVEL);
                ordemSetorRepository.save(novaEtapa);
            }

            int restante = ordemOriginal.getQtdPendente() - somaDivisoes;

            if (restante > 0) {
                // A sobra mantém a variante que a linha já tinha (null quando não havia divisão)
                ordemOriginal.setQtdRecebida(restante);
                ordemOriginal.setQtdPendente(restante);
                ordemSetorRepository.save(ordemOriginal);
            } else {
                ordemSetorRepository.delete(ordemOriginal);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao dividir etapa da ordem setor id {}: {}", ordemSetorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao dividir etapa da ordem setor id {}: {}", ordemSetorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

}
