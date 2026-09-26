package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.dashboard.DashboardOrdemDTO;
import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.RoteiroProduto;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final RoteiroProdutoRepository roteiroProdutoRepository;
    private final OrdemSetorRepository ordemSetorRepository;
    private final RefugoRepository refugoRepository;

    @Transactional(readOnly = true)
    public DashboardResumoDTO obterResumoEstatisticas() {
        try {
            Long totalAtivas = ordemSetorRepository.countByStatusNot(EstatusOrdemProducao.CONCLUIDA);
            Long etapasEmAndamento = ordemSetorRepository.countByStatus(EstatusOrdemProducao.EM_ANDAMENTO);
            Long concluidasHoje = ordemSetorRepository.countByStatusAndFimBetween(
                    EstatusOrdemProducao.CONCLUIDA,
                    LocalDate.now().atStartOfDay(),
                    LocalDate.now().atTime(23, 59, 59));
            Long pecasEmRetrabalho = contarPecasEmRetrabalhoAberto();

            return new DashboardResumoDTO(
                    totalAtivas != null ? totalAtivas : 0L,
                    etapasEmAndamento != null ? etapasEmAndamento : 0L,
                    concluidasHoje != null ? concluidasHoje : 0L,
                    pecasEmRetrabalho != null ? pecasEmRetrabalho : 0L
            );
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao obter resumo de estatísticas do dashboard: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao obter resumo de estatísticas do dashboard: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<SetorResumoDTO> obterResumoSetores() {
        try {
            List<SetorResumoDTO> resumo = ordemSetorRepository.contarEtapasDisponiveisPorSetor();
            return resumo != null ? resumo : List.of();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao obter resumo de setores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao obter resumo de setores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<DashboardOrdemDTO> listarOrdensAtivas(int limite) {
        try {
            List<OrdemProducao> ordens = ordemProducaoRepository
                    .findByStatusNotOrderByDataCriacaoDesc(EstatusOrdemProducao.CONCLUIDA, PageRequest.of(0, limite));

            if (ordens == null || ordens.isEmpty()) {
                return List.of();
            }

            List<Long> ordemIds = ordens.stream().map(OrdemProducao::getId).toList();
            Set<Long> produtoIds = ordens.stream()
                    .filter(o -> o.getProduto() != null && o.getProduto().getId() != null)
                    .map(o -> o.getProduto().getId())
                    .collect(Collectors.toSet());

            Map<Long, List<OrdemSetor>> etapasPorOrdem = ordemSetorRepository
                    .findByOrdemProducaoIdInOrderBySequenciaAsc(ordemIds).stream()
                    .collect(Collectors.groupingBy(os -> os.getOrdemProducao().getId()));

            Map<Long, List<RoteiroProduto>> roteiroPorProduto = produtoIds.isEmpty() ? Map.of() : roteiroProdutoRepository
                    .findByProdutoIdIn(produtoIds).stream()
                    .filter(rp -> rp.getProduto() != null && rp.getProduto().getId() != null)
                    .collect(Collectors.groupingBy(rp -> rp.getProduto().getId()));

            return ordens.stream()
                    .map(ordem -> montarDashboardOrdemDTO(
                            ordem,
                            etapasPorOrdem.getOrDefault(ordem.getId(), List.of()),
                            ordem.getProduto() != null ? roteiroPorProduto.getOrDefault(ordem.getProduto().getId(), List.of()) : List.of()
                    ))
                    .toList();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar ordens ativas para o dashboard (limite {}): {}", limite, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar ordens ativas para o dashboard (limite {}): {}", limite, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
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
            if (etapaAtual.getSetor() != null) {
                setorNome = etapaAtual.getSetor().getNome();
            }
            etapaNome = roteiro.stream()
                    .filter(rp -> rp.getSequencia().equals(etapaAtual.getSequencia()) && rp.getEtapaSetor() != null)
                    .map(rp -> rp.getEtapaSetor().getNome())
                    .findFirst()
                    .orElse(null);
        }

        return new DashboardOrdemDTO(
                ordem.getId(),
                ordem.getNumero(),
                ordem.getProduto() != null ? ordem.getProduto().getNome() : null,
                ordem.getQuantidade(),
                progresso,
                etapaNome,
                setorNome,
                ordem.getStatus() != null ? ordem.getStatus().name() : null
        );
    }

    @Transactional(readOnly = true)
    public Long contarPecasEmRetrabalhoAberto() {
        try {
            List<Object[]> resultados = refugoRepository.buscarRefugosComTotalRetrabalhado();

            if (resultados == null || resultados.isEmpty()) {
                return 0L;
            }

            long total = 0;
            for (Object[] row : resultados) {
                if (row != null && row.length >= 3) {
                    Integer qtdRefugo = row[1] instanceof Integer ? (Integer) row[1] : 0;
                    Long qtdRetrabalhada = row[2] instanceof Long ? (Long) row[2] : 0L;
                    long pendente = qtdRefugo - qtdRetrabalhada;
                    if (pendente > 0) {
                        total += pendente;
                    }
                }
            }
            return total;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao contar peças em retrabalho aberto: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao contar peças em retrabalho aberto: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}