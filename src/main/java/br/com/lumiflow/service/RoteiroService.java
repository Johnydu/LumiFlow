package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.comum.PaginaDTO;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.roteiro.PassoRoteiroDTO;
import br.com.lumiflow.dto.roteiro.ProdutoVinculoDTO;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroListagemDTO;
import br.com.lumiflow.entity.Produto;
import br.com.lumiflow.entity.Roteiro;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.entity.RoteiroPasso;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.ProdutoRepository;
import br.com.lumiflow.repository.RoteiroPassoRepository;
import br.com.lumiflow.repository.RoteiroRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoteiroService {

    private static final int TAMANHO_PAGINA_VINCULO = 20;

    private final RoteiroRepository roteiroRepository;
    private final RoteiroPassoRepository roteiroPassoRepository;
    private final ProdutoRepository produtoRepository;
    private final SetorService setorService;
    private final EtapaSetorService etapaSetorService;
    private final OrdemProducaoRepository ordemProducaoRepository;

    // ==================== CONSULTAS ====================

    @Transactional(readOnly = true)
    public List<RoteiroListagemDTO> listarRoteiros(String termo) {
        log.debug("Listando roteiros com termo de busca: [{}]", termo);
        String busca = (termo == null || termo.isBlank()) ? null : termo.trim();
        List<Roteiro> roteiros = roteiroRepository.buscar(busca);
        if (roteiros.isEmpty()) {
            log.debug("Nenhum roteiro encontrado para o termo de busca: [{}]", termo);
            return List.of();
        }

        List<Long> ids = roteiros.stream().map(Roteiro::getId).toList();

        Map<Long, List<RoteiroPasso>> passosPorRoteiro = roteiroPassoRepository.buscarPassosDosRoteiros(ids).stream()
                .collect(Collectors.groupingBy(p -> p.getRoteiro().getId()));
        Map<Long, Long> produtosPorRoteiro = new HashMap<>();
        produtoRepository.contarPorRoteiros(ids).forEach(row ->
                produtosPorRoteiro.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue()));

        log.debug("Total de roteiros encontrados: [{}]", roteiros.size());
        return roteiros.stream().map(r -> {
            List<RoteiroPasso> passos = passosPorRoteiro.getOrDefault(r.getId(), List.of());
            return new RoteiroListagemDTO(
                    r.getId(), r.getCodigo(), r.getDescricao(), passos.size(),
                    passos.stream().map(this::descreverPasso).toList(),
                    produtosPorRoteiro.getOrDefault(r.getId(), 0L));
        }).toList();
    }

    @Transactional(readOnly = true)
    public RoteiroDTO buscarPorId(Long id) {
        log.debug("Buscando roteiro por ID: [{}]", id);
        Roteiro roteiro = buscarEntidade(id);
        List<PassoRoteiroDTO> passos = roteiroPassoRepository.buscarPassosComSetorEEtapa(id).stream()
                .map(p -> new PassoRoteiroDTO(p.getSetor().getId(),
                        p.getEtapaSetor() == null ? null : p.getEtapaSetor().getId()))
                .toList();
        return new RoteiroDTO(roteiro.getId(), roteiro.getCodigo(), roteiro.getDescricao(), passos);
    }

    @Transactional(readOnly = true)
    public PaginaDTO<ProdutoVinculoDTO> buscarProdutosParaVinculo(Long roteiroId, String termo,
                                                                  boolean somenteVinculados, int pagina) {
        buscarEntidade(roteiroId);
        String busca = termo == null ? "" : termo.trim();
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), TAMANHO_PAGINA_VINCULO);
        Page<Produto> resultado = somenteVinculados
                ? produtoRepository.buscarVinculados(roteiroId, busca, pageable)
                : produtoRepository.buscarPorTermo(busca, pageable);

        List<ProdutoVinculoDTO> itens = resultado.getContent().stream()
                .map(p -> new ProdutoVinculoDTO(
                        p.getId(),
                        p.getNome(),
                        p.getCodigo(),
                        p.getRoteiro() == null ? null : p.getRoteiro().getCodigo(),
                        p.getRoteiro() != null && p.getRoteiro().getId().equals(roteiroId)))
                .toList();

        return new PaginaDTO<>(itens, resultado.getNumber(), resultado.hasNext(), resultado.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<String> buscarFluxoPorProdutoId(Long produtoId) {
        log.debug("Buscando fluxo de produção para o produto ID: [{}]", produtoId);
        if (produtoId == null) return List.of();
        return roteiroPassoRepository.buscarPorProdutoId(produtoId).stream()
                .map(this::descreverPasso)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EtapaSetorDTO> buscarEtapasDoSetorNoRoteiro(Long roteiroId, Long setorId) {
        log.debug("Buscando etapas do setor ID: [{}] no roteiro ID: [{}]", setorId, roteiroId);
        return roteiroPassoRepository.buscarPassosComSetorEEtapa(roteiroId).stream()
                .filter(rp -> rp.getSetor().getId().equals(setorId) && rp.getEtapaSetor() != null)
                .map(rp -> new EtapaSetorDTO(
                        rp.getEtapaSetor().getId(),
                        rp.getEtapaSetor().getNome(),
                        rp.getEtapaSetor().getOrdem()))
                .toList();
    }

    // ==================== ESCRITA ====================

    @Transactional
    public void criar(RoteiroDTO dto) {
        log.info("Iniciando criação de novo roteiro com código: [{}]", dto.codigo());
        String codigo = normalizarCodigo(dto.codigo());
        if (roteiroRepository.existsByCodigoIgnoreCase(codigo)) {
            log.warn("Tentativa de criar roteiro com código duplicado: [{}]", codigo);
            throw new BusinessException(AppMessages.ERROR_ROUTE_CODE_DUPLICATE);
        }

        Roteiro roteiro = new Roteiro();
        roteiro.setCodigo(codigo);
        roteiro.setDescricao(normalizarDescricao(dto.descricao()));
        roteiroRepository.save(roteiro);

        salvarPassos(roteiro, dto.passos());
        log.info("Roteiro criado com sucesso ID: [{}] e código: [{}]", roteiro.getId(), roteiro.getCodigo());
    }

    @Transactional
    public void atualizar(Long id, RoteiroDTO dto) {
        log.info("Iniciando atualização do roteiro ID: [{}]", id);
        Roteiro roteiro = buscarEntidadeParaAtualizacao(id);
        String codigo = normalizarCodigo(dto.codigo());
        if (roteiroRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            log.warn("Tentativa de atualizar para um código de roteiro já existente: [{}]", codigo);
            throw new BusinessException(AppMessages.ERROR_ROUTE_CODE_DUPLICATE);
        }

        roteiro.setCodigo(codigo);
        roteiro.setDescricao(normalizarDescricao(dto.descricao()));

        List<RoteiroPasso> atuais = roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(id);
        if (!mesmaEstrutura(atuais, dto.passos())) {
            log.debug("Alteração estrutural detectada nos passos do roteiro ID: [{}]", id);
            if (ordemProducaoRepository.existsByRoteiroId(id)) {
                log.warn("Falha ao alterar estrutura do roteiro ID: [{}]: já existem ordens que o utilizam", id);
                throw new BusinessException(AppMessages.ERROR_ROUTE_IN_USE);
            }
            roteiroPassoRepository.deletarPorRoteiroId(id);
            salvarPassos(roteiro, dto.passos());
        }
        log.info("Roteiro ID: [{}] atualizado com sucesso", id);
    }

    @Transactional
    public void duplicar(Long id, String novoCodigo) {
        log.info("Iniciando duplicação do roteiro ID: [{}] para o novo código: [{}]", id, novoCodigo);
        Roteiro origem = buscarEntidade(id);
        String codigo = normalizarCodigo(novoCodigo);
        if (roteiroRepository.existsByCodigoIgnoreCase(codigo)) {
            log.warn("Tentativa de duplicar roteiro com código já existente: [{}]", codigo);
            throw new BusinessException(AppMessages.ERROR_ROUTE_CODE_DUPLICATE);
        }

        Roteiro copia = new Roteiro();
        copia.setCodigo(codigo);
        copia.setDescricao(origem.getDescricao());
        roteiroRepository.save(copia);

        List<RoteiroPasso> passosCopiados = roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(id).stream()
                .map(p -> {
                    RoteiroPasso novo = new RoteiroPasso();
                    novo.setRoteiro(copia);
                    novo.setSetor(p.getSetor());
                    novo.setEtapaSetor(p.getEtapaSetor());
                    novo.setSequencia(p.getSequencia());
                    return novo;
                }).toList();
        roteiroPassoRepository.saveAll(passosCopiados);
        log.info("Roteiro ID: [{}] duplicado com sucesso para o novo ID: [{}] e código: [{}]", id, copia.getId(), copia.getCodigo());
    }

    @Transactional
    public void excluir(Long id) {
        log.info("Solicitação de exclusão do roteiro ID: [{}]", id);
        Roteiro roteiro = buscarEntidade(id);
        if (produtoRepository.countByRoteiroId(id) > 0) {
            log.warn("Tentativa de excluir roteiro ID: [{}] que possui produtos vinculados", id);
            throw new BusinessException(AppMessages.ERROR_ROUTE_HAS_PRODUCTS);
        }
        if (ordemProducaoRepository.existsByRoteiroId(id)) {
            log.warn("Tentativa de excluir roteiro ID: [{}] que já foi usado em ordens de produção", id);
            throw new BusinessException(AppMessages.ERROR_ROUTE_IN_USE);
        }
        roteiroRepository.delete(roteiro);
        log.info("Roteiro ID: [{}] excluído com sucesso", id);
    }

    @Transactional
    public void vincularProdutos(Long roteiroId, Collection<Long> adicionar, Collection<Long> remover) {
        log.info("Atualizando vínculo de produtos para o roteiro ID: [{}]", roteiroId);
        Roteiro roteiro = buscarEntidade(roteiroId);
        Set<Long> idsAdicionar = adicionar == null ? new HashSet<>() : new HashSet<>(adicionar);
        Set<Long> idsRemover = remover == null ? new HashSet<>() : new HashSet<>(remover);

        Set<Long> contraditorios = new HashSet<>(idsAdicionar);
        contraditorios.retainAll(idsRemover);
        idsAdicionar.removeAll(contraditorios);
        idsRemover.removeAll(contraditorios);

        if (!idsRemover.isEmpty()) {
            produtoRepository.findAllById(idsRemover).stream()
                    .filter(p -> p.getRoteiro() != null && p.getRoteiro().getId().equals(roteiroId))
                    .forEach(p -> p.setRoteiro(null));
        }

        if (!idsAdicionar.isEmpty()) {
            List<Produto> produtos = produtoRepository.findAllById(idsAdicionar);
            if (produtos.size() != idsAdicionar.size()) {
                log.warn("Um ou mais produtos informados para vínculo não foram encontrados no banco de dados");
                throw new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND);
            }
            produtos.forEach(p -> p.setRoteiro(roteiro));
        }
        log.info("Vínculo de produtos atualizado com sucesso para o roteiro ID: [{}]", roteiroId);
    }

    // ==================== APOIO ====================

    private Roteiro buscarEntidade(Long id) {
        return roteiroRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Roteiro com ID: [{}] não foi encontrado", id);
                    return new BusinessException(AppMessages.ERROR_ROUTE_NOTFOUND);
                });
    }

    /** Mesma busca, mas travando o roteiro: a edição estrutural não corre junto com a criação de ordens. */
    private Roteiro buscarEntidadeParaAtualizacao(Long id) {
        return roteiroRepository.findByIdForUpdate(id)
                .orElseThrow(() -> {
                    log.warn("Roteiro com ID: [{}] não foi encontrado", id);
                    return new BusinessException(AppMessages.ERROR_ROUTE_NOTFOUND);
                });
    }

    private boolean mesmaEstrutura(List<RoteiroPasso> atuais, List<PassoRoteiroDTO> novos) {
        if (novos == null || atuais.size() != novos.size()) return false;
        for (int i = 0; i < atuais.size(); i++) {
            RoteiroPasso atual = atuais.get(i);
            PassoRoteiroDTO novo = novos.get(i);
            Long etapaAtualId = atual.getEtapaSetor() == null ? null : atual.getEtapaSetor().getId();
            if (!atual.getSetor().getId().equals(novo.setorId()) || !Objects.equals(etapaAtualId, novo.etapaSetorId())) {
                return false;
            }
        }
        return true;
    }

    private void salvarPassos(Roteiro roteiro, List<PassoRoteiroDTO> passos) {
        if (passos == null || passos.isEmpty()) {
            log.warn("Tentativa de salvar roteiro sem etapas definidas");
            throw new BusinessException(AppMessages.ERROR_ROUTE_STEP_REQUIRED);
        }

        List<RoteiroPasso> novos = new ArrayList<>();
        int sequencia = 10;
        for (PassoRoteiroDTO passo : passos) {
            if (passo == null || passo.setorId() == null) {
                log.warn("Passo de roteiro inválido ou sem setor informado");
                throw new BusinessException(AppMessages.ERROR_ROUTE_STEP_REQUIRED);
            }
            RoteiroPasso item = new RoteiroPasso();
            item.setRoteiro(roteiro);
            item.setSetor(setorService.buscarSetorPorId(passo.setorId()));
            item.setSequencia(sequencia);

            if (passo.etapaSetorId() != null) {
                var etapa = Optional.ofNullable(etapaSetorService.buscarPorId(passo.etapaSetorId()))
                        .orElseThrow(() -> {
                            log.warn("Etapa de setor ID: [{}] não encontrada ao configurar passos do roteiro", passo.etapaSetorId());
                            return new BusinessException(AppMessages.ERROR_ROUTE_STEP_NOTFOUND, new Object[]{passo.etapaSetorId()});
                        });
                if (!etapa.getSetor().getId().equals(passo.setorId())) {
                    log.warn("A etapa informada não pertence ao setor selecionado");
                    throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
                }
                item.setEtapaSetor(etapa);
            }
            novos.add(item);
            sequencia += 10;
        }
        roteiroPassoRepository.saveAll(novos);
        log.debug("Passos salvos com sucesso para o roteiro ID: [{}]", roteiro.getId());
    }

    private String descreverPasso(RoteiroPasso passo) {
        return passo.getEtapaSetor() == null
                ? passo.getSetor().getNome()
                : passo.getSetor().getNome() + " (" + passo.getEtapaSetor().getNome() + ")";
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarDescricao(String descricao) {
        return (descricao == null || descricao.isBlank()) ? null : descricao.trim();
    }
}
