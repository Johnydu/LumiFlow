package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.dto.vidracaria.HistoricoMovimentacaoDTO;
import br.com.lumiflow.dto.vidracaria.MovimentacaoVidroDTO;
import br.com.lumiflow.dto.vidracaria.SaldoVidroDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.ChapaVidroMapper;
import br.com.lumiflow.model.ChapaVidro;
import br.com.lumiflow.model.MovimentacaoVidro;
import br.com.lumiflow.model.enums.TipoMovimentacao;
import br.com.lumiflow.repository.ChapaVidroRepository;
import br.com.lumiflow.repository.MovimentacaoVidroRepository;
import br.com.lumiflow.repository.OperadorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service @AllArgsConstructor
public class VidracariaService {
    private final ChapaVidroRepository chapaVidroRepository; private final MovimentacaoVidroRepository movimentacaoVidroRepository; private final ChapaVidroMapper chapaVidroMapper; private final OperadorRepository operadorRepository;
    public List<ChapaVidro> listarChapas() { return chapaVidroRepository.findAll(); }
    @Transactional(readOnly = true) public List<SaldoVidroDTO> obterSaldosAtuais() { return chapaVidroRepository.findAll().stream().map(chapa -> { int entradas = movimentacaoVidroRepository.somarQuantidadePorChapaETipo(chapa.getId(), TipoMovimentacao.ENTRADA); int consumos = movimentacaoVidroRepository.somarQuantidadePorChapaETipo(chapa.getId(), TipoMovimentacao.SAIDA); int saldo = entradas - consumos; return new SaldoVidroDTO(chapa.getId(), chapa.getTipoVidro(), chapa.getDescricao(), saldo, chapa.getEstoqueMinimo(), saldo <= chapa.getEstoqueMinimo()); }).toList(); }
    @Transactional(readOnly = true) public List<HistoricoMovimentacaoDTO> listarHistorico() { return movimentacaoVidroRepository.findAllByOrderByDataHoraDesc().stream().map(m -> new HistoricoMovimentacaoDTO(m.getId(), m.getTipoMovimentacao(), m.getChapaVidro().getTipoVidro(), m.getChapaVidro().getDescricao(), m.getQuantidade(), m.getDataHora(), m.getOperador() == null ? "Sistema" : m.getOperador().getNome(), m.getObservacao())).toList(); }
    @Transactional public void registrarMovimentacao(MovimentacaoVidroDTO dto) { var chapa = chapaVidroRepository.findById(dto.chapaVidroId()).orElseThrow(() -> new BusinessException(AppMessages.ERROR_GLASS_SHEET_NOTFOUND)); var operador = operadorRepository.findById(dto.operadorId()).orElseThrow(() -> new BusinessException(AppMessages.ERROR_GLASS_OPERATOR_NOTFOUND)); var movimentacao = new MovimentacaoVidro(); movimentacao.setChapaVidro(chapa); movimentacao.setQuantidade(dto.quantidade()); movimentacao.setOperador(operador); movimentacao.setTipoMovimentacao(dto.tipoMovimentacao()); movimentacao.setDataHora(dto.dataHora() == null ? LocalDateTime.now() : dto.dataHora()); movimentacao.setObservacao(dto.observacao()); movimentacaoVidroRepository.save(movimentacao); }
    @Transactional public void cadastrarChapa(ChapaVidroDTO dto) { if (chapaVidroRepository.existsByTipoVidro(dto.tipoVidro())) throw new BusinessException(AppMessages.ERROR_GLASS_TYPE_DUPLICATE); chapaVidroRepository.save(chapaVidroMapper.toEntity(dto)); }
}
