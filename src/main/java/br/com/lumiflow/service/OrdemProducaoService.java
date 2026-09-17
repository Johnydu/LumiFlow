package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.*;
import br.com.lumiflow.entity.*;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.OrdemProducaoMapper;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import br.com.lumiflow.repository.RoteiroProdutoRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
public class OrdemProducaoService {

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final ProdutoService produtoService;
    private final RoteiroProdutoRepository roteiroProdutoRepository;
    private final OrdemSetorRepository ordemSetorRepository;
    private final OrdemProducaoMapper ordemProducaoMapper;



    @Transactional(readOnly = true)
    public List<OrdemListagemDTO> listarPorSetorIdEFiltros(Long setorId, OrdemFiltroDTO filtro) {
        String busca = (filtro.busca() != null && !filtro.busca().isBlank()) ? filtro.busca().trim() : null;
        String status = (filtro.status() != null && !filtro.status().isBlank()) ? filtro.status().trim() : null;

        List<OrdemSetor> etapas = ordemSetorRepository.buscarPorSetorIdEFiltros(setorId, busca, status);

        if (etapas.isEmpty()) {
            return List.of();
        }

        Set<Long> produtoIds = etapas.stream()
                .map(os -> os.getOrdemProducao().getProduto().getId())
                .collect(Collectors.toSet());

        Map<String, String> etapaNomePorProdutoESequencia = roteiroProdutoRepository
                .findByProdutoIdIn(produtoIds)
                .stream()
                .filter(rp -> rp.getEtapaSetor() != null)
                .collect(Collectors.toMap(
                        rp -> rp.getProduto().getId() + "-" + rp.getSequencia(),
                        rp -> rp.getEtapaSetor().getNome(),
                        (a, b) -> a // em caso de chave duplicada, mantém a primeira
                ));

        return etapas.stream()
                .map(os -> montarOrdemListagemDTO(os, etapaNomePorProdutoESequencia))
                .toList();
    }

    private OrdemListagemDTO montarOrdemListagemDTO(OrdemSetor ordemSetor, Map<String,
            String> etapaNomePorProdutoESequencia) {

        OrdemProducao ordem = ordemSetor.getOrdemProducao();
        Integer totalEtapa = ordemSetor.getQtdRecebida();
        Integer produzido = ordemSetor.getQtdProduzida();
        Double percentual = totalEtapa == 0 ? 0.0 : (produzido * 100.0) / totalEtapa;

        String chave = ordem.getProduto().getId() + "-" + ordemSetor.getSequencia();
        String etapaNome = etapaNomePorProdutoESequencia.get(chave);

        return new OrdemListagemDTO(
                ordem.getId(),
                ordemSetor.getId(),
                ordem.getNumero(),
                ordem.getProduto().getNome(),
                ordem.getProduto().getCodigo(),
                etapaNome,
                ordemSetor.getVariante(),
                totalEtapa,
                produzido,
                percentual,
                ordem.getObservacao(),
                ordem.getCriadoEm(),
                ordemSetor.getStatus()
        );
    }

    public Page<OrdemProducaoDTO> listarOrdens(int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho);
        return ordemProducaoRepository.findAllOrdenadasPorEmAndamento(pageable)
                .map(ordemProducaoMapper::toDTO);
    }

    @Transactional
    public void criar(OrdemProducaoDTO dto, Usuario usuarioLogado) {
        var produto = produtoService.buscarProdutoPorId(dto.produtoId());

        OrdemProducao ordem = new OrdemProducao();
        ordem.setNumero(gerarNumero());
        ordem.setQuantidade(dto.quantidade());
        ordem.setStatus(EstatusOrdemProducao.DISPONIVEL);
        ordem.setProduto(produto);
        ordem.setCriadoPor(usuarioLogado);
        ordem.setDataCriacao(dto.dataCriacao().atStartOfDay());
        ordem.setObservacao(dto.observacao());

        ordemProducaoRepository.save(ordem);

        criarPrimeiraEtapaOrdemSetor(ordem);
    }

    private void criarPrimeiraEtapaOrdemSetor(OrdemProducao ordem) {
        List<RoteiroProduto> roteiro = roteiroProdutoRepository.buscarPorProdutoId(ordem.getProduto().getId());

        if (roteiro.isEmpty()) {
            throw new BusinessException(AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE);
        }

        RoteiroProduto primeiroPasso = roteiro.getFirst(); // já vem ordenado por sequência

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
        return "OP-" + String.format("%04d", seq);
    }

    @Transactional
    public void liberarOrdem(Long ordemSetorId) {
        OrdemSetor ordemSetor = ordemSetorRepository.findById(ordemSetorId)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND));

        if (ordemSetor.getStatus() != EstatusOrdemProducao.DISPONIVEL) {
            throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        ordemSetor.setStatus(EstatusOrdemProducao.LIBERADA);
        ordemSetorRepository.save(ordemSetor);
    }

    @Transactional
    public void dividirEtapa(Long ordemSetorId, DividirEtapaDTO dto) {
        OrdemSetor ordemOriginal = ordemSetorRepository.findById(ordemSetorId)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND));

        if (ordemOriginal.getStatus() != EstatusOrdemProducao.DISPONIVEL) {
            throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        int somaDivisoes = dto.divisoes().stream().mapToInt(DivisaoItemDTO::quantidade).sum();

        if (somaDivisoes > ordemOriginal.getQtdPendente()) {
            throw new BusinessException(AppMessages.ERROR_DIVISION_SUM_MISMATCH);
        }

        OrdemProducao ordem = ordemOriginal.getOrdemProducao();
        Setor setor = ordemOriginal.getSetor();
        Integer sequencia = ordemOriginal.getSequencia();

        for (DivisaoItemDTO item : dto.divisoes()) {
            OrdemSetor novaEtapa = new OrdemSetor();
            novaEtapa.setOrdemProducao(ordem);
            novaEtapa.setSetor(setor);
            novaEtapa.setSequencia(sequencia);
            novaEtapa.setVariante(item.variante().toUpperCase(Locale.ROOT));
            novaEtapa.setQtdRecebida(item.quantidade());
            novaEtapa.setQtdProduzida(0);
            novaEtapa.setQtdPendente(item.quantidade());
            novaEtapa.setStatus(EstatusOrdemProducao.DISPONIVEL);
            ordemSetorRepository.save(novaEtapa);
        }

        int restante = ordemOriginal.getQtdPendente() - somaDivisoes;

        if (restante > 0) {
            // Sobra fica como uma linha "sem variante" — o próprio registro original é reaproveitado
            ordemOriginal.setQtdRecebida(restante);
            ordemOriginal.setQtdPendente(restante);
            ordemOriginal.setVariante(null);
            ordemSetorRepository.save(ordemOriginal);
        } else {
            ordemSetorRepository.delete(ordemOriginal);
        }
    }


}