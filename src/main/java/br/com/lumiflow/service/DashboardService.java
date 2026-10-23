package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.dashboard.DashboardOrdemDTO;
import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.dto.roteiro.PassoDoRoteiroDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.Roteiro;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.entity.enums.DestinoRefugo;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final RoteiroPassoRepository roteiroPassoRepository;
    private final OrdemSetorRepository ordemSetorRepository;
    private final RefugoRepository refugoRepository;
    private final RetrabalhoRepository retrabalhoRepository;

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
            Set<Long> roteiroIds = ordens.stream()
                    .map(OrdemProducao::getRoteiro)
                    .filter(Objects::nonNull)
                    .map(Roteiro::getId)
                    .collect(Collectors.toSet());

            Map<Long, List<OrdemSetor>> etapasPorOrdem = ordemSetorRepository
                    .findByOrdemProducaoIdInOrderBySequenciaAsc(ordemIds).stream()
                    .collect(Collectors.groupingBy(os -> os.getOrdemProducao().getId()));

            Map<Long, List<PassoDoRoteiroDTO>> passosPorRoteiro = roteiroIds.isEmpty() ? Map.of() : roteiroPassoRepository
                    .buscarPassosPorRoteiroIds(roteiroIds).stream()
                    .collect(Collectors.groupingBy(PassoDoRoteiroDTO::roteiroId));

            return ordens.stream()
                    .map(ordem -> montarDashboardOrdemDTO(
                            ordem,
                            etapasPorOrdem.getOrDefault(ordem.getId(), List.of()),
                            ordem.getRoteiro() != null ? passosPorRoteiro.getOrDefault(ordem.getRoteiro().getId(), List.of()) : List.of()
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

    private DashboardOrdemDTO montarDashboardOrdemDTO(OrdemProducao ordem, List<OrdemSetor> etapas, List<PassoDoRoteiroDTO> roteiro) {

        long totalEtapas = roteiro.stream().map(PassoDoRoteiroDTO::sequencia).distinct().count();

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
            // Filtra e pega etapaNome() direto do PassoDoRoteiroDTO
            etapaNome = roteiro.stream()
                    .filter(pp -> pp.sequencia().equals(etapaAtual.getSequencia()) && pp.etapaNome() != null)
                    .map(PassoDoRoteiroDTO::etapaNome)
                    .findFirst()
                    .orElse(null);
        }

        return new DashboardOrdemDTO(
                ordem.getId(),
                ordem.getNumero(),
                ordem.getProduto() != null ? ordem.getProduto().getNome() : null,
                ordem.getProduto() != null ? ordem.getProduto().getCodigo() : null,
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
            Long refugos = refugoRepository.somarQuantidadePorDestino(DestinoRefugo.RETRABALHO);
            Long refeitas = retrabalhoRepository.somarQuantidadePorDestino(DestinoRefugo.RETRABALHO);
            return Math.max(0L, refugos - refeitas);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao contar peças em retrabalho aberto: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao contar peças em retrabalho aberto: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}
