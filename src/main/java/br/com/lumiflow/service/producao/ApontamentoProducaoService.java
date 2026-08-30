package br.com.lumiflow.service.producao;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.apontamento.ApontamentoRequestDTO;
import br.com.lumiflow.entity.*;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;


@Service
@RequiredArgsConstructor
public class ApontamentoProducaoService {

    private final OrdemSetorRepository ordemSetorRepository;
    private final LancamentoRepository lancamentoRepository;
    private final RefugoRepository refugoRepository;
    private final MaquinaRepository maquinaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidadorApontamento validador;
    private final AvancoRoteiroService avancoRoteiroService;
    private final ConclusaoOrdemService conclusaoOrdemService;

    @Transactional
    @PreAuthorize("hasAnyRole('OPERADOR','PCP_SUPERVISOR','GESTAO','SUPORTE')")
    public void lancar(ApontamentoRequestDTO dto, Long usuarioLogadoId) {

        OrdemSetor ordemSetor = ordemSetorRepository.findById(dto.ordemSetorId())
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDEM_SETOR_NOTFOUND));

        validador.validar(ordemSetor, dto);

        Usuario usuario = usuarioRepository.getReferenceById(usuarioLogadoId);
        Maquina maquina = maquinaRepository.getReferenceById(dto.maquinaId());
        int qtdRefugo = dto.qtdRefugo() != null ? dto.qtdRefugo() : 0;

        Lancamento lancamento = new Lancamento();
        lancamento.setOrdemSetor(ordemSetor);
        lancamento.setMaquina(maquina);
        lancamento.setUsuario(usuario);
        lancamento.setQtdProduzida(dto.qtdProduzida());
        lancamento.setDataHora(OffsetDateTime.now().toLocalDateTime());
        lancamentoRepository.save(lancamento);

        if (qtdRefugo > 0) {
            Refugo refugo = new Refugo();
            refugo.setOrdemSetor(ordemSetor);
            refugo.setSetorOrigem(ordemSetor.getSetor());
            refugo.setUsuario(usuario);
            refugo.setQtdRefugo(qtdRefugo);
            refugo.setMotivo(dto.motivoRefugo());
            refugo.setDestino(DestinoRefugo.DESCARTE);
            refugo.setDataHora(OffsetDateTime.now().toLocalDateTime());
            refugoRepository.save(refugo);
        }

        int totalLancado = dto.qtdProduzida() + qtdRefugo;
        ordemSetor.setQtdProduzida(ordemSetor.getQtdProduzida() + dto.qtdProduzida());
        ordemSetor.setQtdPendente(ordemSetor.getQtdPendente() - totalLancado);

        if (ordemSetor.getQtdPendente() == 0) {
            ordemSetor.setStatus(EstatusOrdemProducao.CONCLUIDA);
            ordemSetor.setFim(OffsetDateTime.now().toLocalDateTime());
        } else {
            ordemSetor.setStatus(EstatusOrdemProducao.EM_ANDAMENTO);
        }
        ordemSetorRepository.save(ordemSetor);

        if (dto.qtdProduzida() > 0) {
            avancoRoteiroService.avancar(ordemSetor, dto.qtdProduzida());
        }

        conclusaoOrdemService.verificarEFinalizar(ordemSetor.getOrdemProducao());
    }
}