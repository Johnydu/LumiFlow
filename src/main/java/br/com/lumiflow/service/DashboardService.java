package br.com.lumiflow.service;


import br.com.lumiflow.dto.dashboard.DashboardOrdemDTO;
import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.RoteiroProduto;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import br.com.lumiflow.repository.RoteiroProdutoRepository;
import br.com.lumiflow.repository.SetorRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DashboardService {

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final SetorRepository setorRepository;
    private final RoteiroProdutoRepository roteiroProdutoRepository;
    private final OrdemSetorRepository ordemSetorRepository;

    @Transactional(readOnly = true)
    public DashboardResumoDTO obterResumoEstatisticas() {
        Long totalAtivas = ordemProducaoRepository.countByStatusNot(EstatusOrdemProducao.CONCLUIDA);
        Long etapasEmAndamento = ordemSetorRepository.countByStatus(EstatusOrdemProducao.EM_ANDAMENTO);
        Long etapasDisponiveis = ordemSetorRepository.countByStatus(EstatusOrdemProducao.DISPONIVEL);
        Long concluidasHoje = ordemProducaoRepository.countByStatusAndAtualizadoEmBetween(
                EstatusOrdemProducao.CONCLUIDA,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().atTime(23, 59, 59));

        return new DashboardResumoDTO(totalAtivas, etapasEmAndamento, concluidasHoje, etapasDisponiveis);
    }


    @Transactional(readOnly = true)
    public List<SetorResumoDTO> obterResumoSetores() {
        return setorRepository.obterResumoOrdensPorSetor();
    }

    @Transactional(readOnly = true)
    public List<DashboardOrdemDTO> listarOrdensAtivas(int limite) {
        List<OrdemProducao> ordens = ordemProducaoRepository
                .findByStatusNotOrderByDataCriacaoDesc(EstatusOrdemProducao.CONCLUIDA, PageRequest.of(0, limite));

        if (ordens.isEmpty()) return List.of();

        List<Long> ordemIds = ordens.stream().map(OrdemProducao::getId).toList();
        Set<Long> produtoIds = ordens.stream().map(o -> o.getProduto().getId()).collect(Collectors.toSet());

        Map<Long, List<OrdemSetor>> etapasPorOrdem = ordemSetorRepository
                .findByOrdemProducaoIdInOrderBySequenciaAsc(ordemIds).stream()
                .collect(Collectors.groupingBy(os -> os.getOrdemProducao().getId()));

        Map<Long, List<RoteiroProduto>> roteiroPorProduto = roteiroProdutoRepository
                .findByProdutoIdIn(produtoIds).stream()
                .collect(Collectors.groupingBy(rp -> rp.getProduto().getId()));

        return ordens.stream()
                .map(ordem -> montarDashboardOrdemDTO(ordem, etapasPorOrdem.getOrDefault(ordem.getId(), List.of()),
                        roteiroPorProduto.getOrDefault(ordem.getProduto().getId(), List.of())))
                .toList();
    }

    private DashboardOrdemDTO montarDashboardOrdemDTO(OrdemProducao ordem, List<OrdemSetor> etapas, List<RoteiroProduto> roteiro) {
        long totalEtapas = roteiro.stream().map(RoteiroProduto::getSequencia).distinct().count();

        Set<Integer> sequenciasComPendencia = etapas.stream()
                .filter(os -> os.getStatus() != EstatusOrdemProducao.CONCLUIDA)
                .map(OrdemSetor::getSequencia)
                .collect(Collectors.toSet());

        long sequenciasTotais = etapas.stream().map(OrdemSetor::getSequencia).distinct().count();
        long etapasConcluidas = sequenciasTotais - sequenciasComPendencia.size();

        double progresso = totalEtapas == 0 ? 0.0 : (etapasConcluidas * 100.0) / totalEtapas;

        OrdemSetor etapaAtual = etapas.stream()
                .filter(os -> os.getStatus() != EstatusOrdemProducao.CONCLUIDA)
                .max(Comparator.comparing(OrdemSetor::getSequencia))
                .orElse(null);

        String etapaNome = null;
        String setorNome = "Concluída";

        if (etapaAtual != null) {
            setorNome = etapaAtual.getSetor().getNome();
            etapaNome = roteiro.stream()
                    .filter(rp -> rp.getSequencia().equals(etapaAtual.getSequencia()) && rp.getEtapaSetor() != null)
                    .map(rp -> rp.getEtapaSetor().getNome())
                    .findFirst()
                    .orElse(null);
        }

        return new DashboardOrdemDTO(
                ordem.getId(),
                ordem.getNumero(),
                ordem.getProduto().getNome(),
                ordem.getQuantidade(),
                progresso,
                etapaNome,
                setorNome,
                ordem.getStatus().name()
        );
    }
}