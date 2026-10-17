package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.lancamento.HistoricoLancamentoFiltroDTO;
import br.com.lumiflow.dto.lancamento.HistoricoLancamentosDTO;
import br.com.lumiflow.entity.Lancamento;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.LancamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricoLancamentosService {

    private final LancamentoRepository lancamentoRepository;

    @Transactional(readOnly = true)
    public Page<HistoricoLancamentosDTO> listarComFiltros(HistoricoLancamentoFiltroDTO filtro, Pageable pageable) {
        try {
            Long setorId = filtro != null ? filtro.setorId() : null;
            Long etapaSetorId = filtro != null ? filtro.etapaSetorId() : null;
            Long operadorId = filtro != null ? filtro.operadorId() : null;

            return lancamentoRepository.buscarComFiltros(setorId, etapaSetorId, operadorId, pageable)
                    .map(this::toHistoricoDTO);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar histórico de lançamentos com filtros: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar histórico de lançamentos com filtros: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    private HistoricoLancamentosDTO toHistoricoDTO(Lancamento lancamento) {
        return new HistoricoLancamentosDTO(
                lancamento.getOrdemSetor().getOrdemProducao().getId(),
                lancamento.getOrdemSetor().getOrdemProducao().getNumero(),
                lancamento.getOrdemSetor().getOrdemProducao().getProduto().getCodigo(),
                lancamento.getOrdemSetor().getOrdemProducao().getProduto().getNome(),
                lancamento.getOrdemSetor().getSetor().getNome(),
                lancamento.getEtapaSetor() != null
                        ? lancamento.getEtapaSetor().getNome()
                        : String.valueOf(lancamento.getOrdemSetor().getSequencia()),
                lancamento.getOrdemSetor().getVariante(),
                lancamento.getMaquina() != null ? lancamento.getMaquina().getNome() : "—",
                lancamento.getQtdProduzida(),
                lancamento.getObservacao(),
                lancamento.getOperador() != null ? lancamento.getOperador().getNome() : "—",
                lancamento.getDataHora()
        );
    }
}