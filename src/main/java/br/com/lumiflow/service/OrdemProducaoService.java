package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.OrdemFiltroDTO;
import br.com.lumiflow.dto.ordens.OrdemListagemDTO;
import br.com.lumiflow.dto.ordens.OrdemProducaoDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.RoteiroProduto;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
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

@Service
@AllArgsConstructor
public class OrdemProducaoService {

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final ProdutoService produtoService;
    private final RoteiroProdutoRepository roteiroProdutoRepository;
    private final OrdemSetorRepository ordemSetorRepository;

    @Transactional(readOnly = true)
    public List<OrdemProducao> listarPorFiltros(OrdemFiltroDTO filtro) {
        String busca = (filtro.busca() != null && !filtro.busca().isBlank()) ? filtro.busca().trim() : null;
        String status = (filtro.status() != null && !filtro.status().isBlank()) ? filtro.status().trim() : null;

        return ordemProducaoRepository.buscarComFiltros(busca, status);
    }

    @Transactional(readOnly = true)
    public List<OrdemListagemDTO> listarPorSetorIdEFiltros(Long setorId, OrdemFiltroDTO filtro) {
        String busca = (filtro.busca() != null && !filtro.busca().isBlank()) ? filtro.busca().trim() : null;
        String status = (filtro.status() != null && !filtro.status().isBlank()) ? filtro.status().trim() : null;

        List<OrdemProducao> ordens = ordemProducaoRepository.buscarPorSetorIdEFiltros(setorId, busca, status);

        return ordens.stream()
                .map(ordem -> montarOrdemListagemDTO(ordem, setorId))
                .toList();
    }

    private OrdemListagemDTO montarOrdemListagemDTO(OrdemProducao ordem, Long setorId) {
        OrdemSetor ordemSetor = ordemSetorRepository
                .findByOrdemProducaoIdAndSetorId(ordem.getId(), setorId)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND));

        Integer produzido = ordemSetor.getQtdProduzida();
        Double percentual = ordem.getQuantidade() == 0 ? 0.0
                : (produzido * 100.0) / ordem.getQuantidade();

        return new OrdemListagemDTO(
                ordem.getId(),
                ordem.getNumero(),
                ordem.getProduto().getNome(),
                ordem.getQuantidade(),
                produzido,
                percentual,
                ordemSetor.getSetor().getNome(),
                ordem.getCriadoEm(),
                ordemSetor.getStatus()
        );
    }

    public Page<OrdemProducao> listarOrdens(int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho);
        return ordemProducaoRepository.findAllOrdenadasPorEmAndamento(pageable);
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

        RoteiroProduto primeiroPasso = roteiro.get(0); // já vem ordenado por sequência

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
    public void liberarOrdem(Long ordemProducaoId, Long setorId) {
        OrdemSetor ordemSetor = ordemSetorRepository
                .findByOrdemProducaoIdAndSetorId(ordemProducaoId, setorId)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND));

        if (ordemSetor.getStatus() != EstatusOrdemProducao.DISPONIVEL) {
            throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        ordemSetor.setStatus(EstatusOrdemProducao.LIBERADA);
        ordemSetorRepository.save(ordemSetor);
    }
}