package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.dto.vidracaria.HistoricoMovimentacaoDTO;
import br.com.lumiflow.dto.vidracaria.MovimentacaoVidroDTO;
import br.com.lumiflow.dto.vidracaria.SaldoVidroDTO;
import br.com.lumiflow.entity.enums.TipoVidro;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.ChapaVidroMapper;
import br.com.lumiflow.entity.ChapaVidro;
import br.com.lumiflow.entity.MovimentacaoVidro;
import br.com.lumiflow.entity.enums.TipoMovimentacao;
import br.com.lumiflow.repository.ChapaVidroRepository;
import br.com.lumiflow.repository.MovimentacaoVidroRepository;
import br.com.lumiflow.repository.OperadorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VidracariaService {
    private final ChapaVidroRepository chapaVidroRepository;
    private final MovimentacaoVidroRepository movimentacaoVidroRepository;
    private final ChapaVidroMapper chapaVidroMapper;
    private final OperadorRepository operadorRepository;

    @Transactional(readOnly = true)
    public List<ChapaVidro> listarChapas() {
        try {
            return chapaVidroRepository.findAll();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar chapas de vidro: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar chapas de vidro: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<SaldoVidroDTO> obterSaldosAtuais() {
        try {
            return chapaVidroRepository.findAll().stream().map(chapa -> {
                int entradas = movimentacaoVidroRepository.somarQuantidadePorChapaETipo(chapa.getId(),
                        TipoMovimentacao.ENTRADA);
                int consumos = movimentacaoVidroRepository.somarQuantidadePorChapaETipo(chapa.getId(),
                        TipoMovimentacao.SAIDA);
                int saldo = entradas - consumos;
                return new SaldoVidroDTO(chapa.getId(), chapa.getTipoVidro(), chapa.getDescricao(),
                        saldo, chapa.getEstoqueMinimo(), saldo <= chapa.getEstoqueMinimo());
            }).toList();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao obter saldos atuais de vidro: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao obter saldos atuais de vidro: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<HistoricoMovimentacaoDTO> listarHistorico() {
        try {
            return movimentacaoVidroRepository.findAllByOrderByDataHoraDesc().stream()
                    .map(m -> new HistoricoMovimentacaoDTO(m.getId(), m.getTipoMovimentacao(),
                            m.getChapaVidro().getTipoVidro(), m.getChapaVidro().getDescricao(),
                            m.getQuantidade(), m.getDataHora(), m.getOperador() == null ? "Sistema" :
                            m.getOperador().getNome(),
                            m.getObservacao())).toList();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar histórico de movimentações: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar histórico de movimentações: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<HistoricoMovimentacaoDTO> listarHistoricoFiltrado(String tipoMovStr, String tipoVidroStr) {
        try {
            TipoMovimentacao tipoMov = null;
            if (tipoMovStr != null && !tipoMovStr.isBlank()) {
                try {
                    tipoMov = TipoMovimentacao.valueOf(tipoMovStr);
                } catch (IllegalArgumentException ignored) {}
            }

            TipoVidro tipoVidro = null;
            if (tipoVidroStr != null && !tipoVidroStr.isBlank()) {
                try {
                    tipoVidro = TipoVidro.valueOf(tipoVidroStr);
                } catch (IllegalArgumentException ignored) {}
            }

            List<MovimentacaoVidro> movimentacoes = movimentacaoVidroRepository.filtrarHistorico(tipoMov, tipoVidro);

            return movimentacoes.stream()
                    .map(m -> new HistoricoMovimentacaoDTO(
                            m.getId(),
                            m.getTipoMovimentacao(),
                            m.getChapaVidro().getTipoVidro(),
                            m.getChapaVidro().getDescricao(),
                            m.getQuantidade(),
                            m.getDataHora(),
                            m.getOperador() == null ? "Sistema" : m.getOperador().getNome(),
                            m.getObservacao()
                    )).toList();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao filtrar histórico de movimentações: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao filtrar histórico de movimentações: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void registrarMovimentacao(MovimentacaoVidroDTO dto) {
        try {
            if (dto == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            var chapa = chapaVidroRepository.findById(dto.chapaVidroId()).
                    orElseThrow(() -> new BusinessException(AppMessages.ERROR_GLASS_SHEET_NOTFOUND));
            var operador = operadorRepository.findById(dto.operadorId()).
                    orElseThrow(() -> new BusinessException(AppMessages.ERROR_GLASS_OPERATOR_NOTFOUND));

            var movimentacao = new MovimentacaoVidro();
            movimentacao.setChapaVidro(chapa);
            movimentacao.setQuantidade(dto.quantidade());
            movimentacao.setOperador(operador);
            movimentacao.setTipoMovimentacao(dto.tipoMovimentacao());
            movimentacao.setDataHora(dto.dataHora() == null ? LocalDateTime.now() : dto.dataHora());
            movimentacao.setObservacao(dto.observacao());

            movimentacaoVidroRepository.save(movimentacao);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao registrar movimentação de vidro: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao registrar movimentação de vidro: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void cadastrarChapa(ChapaVidroDTO dto) {
        try {
            if (dto == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            if (chapaVidroRepository.existsByTipoVidro(dto.tipoVidro()))
                throw new BusinessException(AppMessages.ERROR_GLASS_TYPE_DUPLICATE);

            chapaVidroRepository.save(chapaVidroMapper.toEntity(dto));
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao cadastrar chapa de vidro: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao cadastrar chapa de vidro: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}