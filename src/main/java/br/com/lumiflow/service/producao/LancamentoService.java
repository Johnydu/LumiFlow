package br.com.lumiflow.service.producao;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.lancamento.LancamentoProducaoDTO;
import br.com.lumiflow.dto.lancamento.RefugoDTO;
import br.com.lumiflow.dto.lancamento.RetrabalhoDTO;
import br.com.lumiflow.entity.*;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.*;
import br.com.lumiflow.service.SetorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LancamentoService {

    private final OrdemSetorRepository ordemSetorRepository;
    private final OrdemProducaoRepository ordemProducaoRepository;
    private final LancamentoRepository lancamentoRepository;
    private final RefugoRepository refugoRepository;
    private final RetrabalhoRepository retrabalhoRepository;
    private final MaquinaRepository maquinaRepository;
    private final RoteiroProdutoRepository roteiroProdutoRepository;
    private final SetorService setorService;

    @Transactional(readOnly = true)
    public OrdemSetor buscarEtapaAtivaPorOrdemProducaoId(Long ordemProducaoId) {
        return ordemSetorRepository
                .findByOrdemProducaoIdAndStatusIn(ordemProducaoId,
                        List.of(EstatusOrdemProducao.LIBERADA, EstatusOrdemProducao.EM_ANDAMENTO))
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE));
    }

    @Transactional
    public void lancarProducao(Long ordemProducaoId, LancamentoProducaoDTO dto, Usuario usuarioLogado) {
        OrdemSetor ordemSetor = buscarEtapaAtivaPorOrdemProducaoId(ordemProducaoId);

        if (dto.quantidade() > ordemSetor.getQtdPendente()) {
            throw new BusinessException(AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);
        }

        Maquina maquina = maquinaRepository.findById(dto.maquinaId())
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND));

        EtapaSetor etapaSetor = roteiroProdutoRepository
                .findByProdutoIdAndSequencia(ordemSetor.getOrdemProducao().getProduto().getId(), ordemSetor.getSequencia())
                .map(RoteiroProduto::getEtapaSetor)
                .orElse(null);

        Lancamento lancamento = new Lancamento();
        lancamento.setQtdProduzida(dto.quantidade());
        lancamento.setObservacao(dto.observacao());
        lancamento.setOrdemSetor(ordemSetor);
        lancamento.setMaquina(maquina);
        lancamento.setUsuario(usuarioLogado);
        lancamento.setEtapaSetor(etapaSetor);
        lancamentoRepository.save(lancamento);

        if (ordemSetor.getInicio() == null) {
            ordemSetor.setInicio(LocalDateTime.now());
        }
        if (ordemSetor.getStatus() == EstatusOrdemProducao.LIBERADA) {
            ordemSetor.setStatus(EstatusOrdemProducao.EM_ANDAMENTO);
        }
        ordemSetor.setQtdProduzida(ordemSetor.getQtdProduzida() + dto.quantidade());
        ordemSetor.setQtdPendente(ordemSetor.getQtdPendente() - dto.quantidade());

        avancarParaProximaEtapa(ordemSetor, dto.quantidade());

        if (ordemSetor.getQtdPendente() == 0) {
            ordemSetor.setStatus(EstatusOrdemProducao.CONCLUIDA);
            ordemSetor.setFim(LocalDateTime.now());
        }

        ordemSetorRepository.save(ordemSetor);
    }

    private void avancarParaProximaEtapa(OrdemSetor ordemSetorAtual, Integer quantidadeLancada) {
        OrdemProducao ordem = ordemSetorAtual.getOrdemProducao();
        List<RoteiroProduto> roteiro = roteiroProdutoRepository.buscarPorProdutoId(ordem.getProduto().getId());

        RoteiroProduto proximoPasso = roteiro.stream()
                .filter(p -> p.getSequencia() > ordemSetorAtual.getSequencia())
                .min(Comparator.comparing(RoteiroProduto::getSequencia))
                .orElse(null);

        if (proximoPasso == null) {
            if (ordemSetorAtual.getQtdPendente() - quantidadeLancada <= 0) {
                ordem.setStatus(EstatusOrdemProducao.CONCLUIDA);
                ordemProducaoRepository.save(ordem);
            }
            return;
        }

        OrdemSetor proximaEtapa = ordemSetorRepository
                .findByOrdemProducaoIdAndSequencia(ordem.getId(), proximoPasso.getSequencia())
                .orElse(null);

        if (proximaEtapa == null) {
            proximaEtapa = new OrdemSetor();
            proximaEtapa.setOrdemProducao(ordem);
            proximaEtapa.setSetor(proximoPasso.getSetor());
            proximaEtapa.setSequencia(proximoPasso.getSequencia());
            proximaEtapa.setQtdRecebida(0);
            proximaEtapa.setQtdProduzida(0);
            proximaEtapa.setQtdPendente(0);
            proximaEtapa.setStatus(EstatusOrdemProducao.DISPONIVEL);
        }

        proximaEtapa.setQtdRecebida(proximaEtapa.getQtdRecebida() + quantidadeLancada);
        proximaEtapa.setQtdPendente(proximaEtapa.getQtdPendente() + quantidadeLancada);
        ordemSetorRepository.save(proximaEtapa);
    }

    @Transactional
    public void registrarRefugo(Long ordemProducaoId, RefugoDTO dto, Usuario usuarioLogado) {
        OrdemSetor ordemSetor = buscarEtapaAtivaPorOrdemProducaoId(ordemProducaoId);
        Setor setorOrigem = setorService.buscarSetorPorId(dto.setorOrigemId());

        Refugo refugo = new Refugo();
        refugo.setQtdRefugo(dto.quantidade());
        refugo.setMotivo(dto.motivo());
        refugo.setDestino(dto.destino());
        refugo.setDataHora(LocalDateTime.now());
        refugo.setOrdemSetor(ordemSetor);
        refugo.setSetorOrigem(setorOrigem);
        refugo.setUsuario(usuarioLogado);

        refugoRepository.save(refugo);
    }

    @Transactional(readOnly = true)
    public List<Refugo> listarRefugosParaRetrabalho(Long ordemProducaoId) {
        return refugoRepository.findByOrdemSetor_OrdemProducao_IdAndDestinoOrderByDataHoraDesc(
                ordemProducaoId, DestinoRefugo.RETRABALHO);
    }

    @Transactional
    public void registrarRetrabalho(RetrabalhoDTO dto, Usuario usuarioLogado) {
        Refugo refugo = refugoRepository.findById(dto.refugoId())
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_REFUGO_NOTFOUND));

        Maquina maquina = maquinaRepository.findById(dto.maquinaId())
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND));

        Retrabalho retrabalho = new Retrabalho();
        retrabalho.setQtdRefeita(dto.quantidade());
        retrabalho.setObservacao(dto.observacao());
        retrabalho.setDataHora(LocalDateTime.now());
        retrabalho.setRefugo(refugo);
        retrabalho.setMaquina(maquina);
        retrabalho.setUsuario(usuarioLogado);

        retrabalhoRepository.save(retrabalho);
    }
}