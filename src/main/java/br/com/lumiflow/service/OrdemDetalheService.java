package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.EtapaOrdemDetalheDTO;
import br.com.lumiflow.dto.ordens.HistoricoOrdemDTO;
import br.com.lumiflow.dto.ordens.OrdemDetalheDTO;
import br.com.lumiflow.dto.ordens.RoteiroPassoDetalheDTO;
import br.com.lumiflow.entity.enums.SituacaoPasso;
import br.com.lumiflow.entity.enums.TipoEventoOrdem;
import br.com.lumiflow.entity.Lancamento;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.Refugo;
import br.com.lumiflow.entity.Retrabalho;
import br.com.lumiflow.entity.RoteiroPasso;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.LancamentoRepository;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import br.com.lumiflow.repository.RefugoRepository;
import br.com.lumiflow.repository.RetrabalhoRepository;
import br.com.lumiflow.repository.RoteiroPassoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Monta a visão detalhada de uma ordem de produção: informações gerais,
 * progresso, roteiro, linhas por setor e histórico de eventos.
 * Somente leitura; não altera nenhum dado.
 */
@Service
@RequiredArgsConstructor
public class OrdemDetalheService {

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final OrdemSetorRepository ordemSetorRepository;
    private final RoteiroPassoRepository roteiroPassoRepository;
    private final LancamentoRepository lancamentoRepository;
    private final RefugoRepository refugoRepository;
    private final RetrabalhoRepository retrabalhoRepository;

    @Transactional(readOnly = true)
    public OrdemDetalheDTO buscarDetalhe(Long ordemId) {
        if (ordemId == null) {
            throw new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND);
        }

        OrdemProducao ordem = ordemProducaoRepository.buscarComProduto(ordemId)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND));

        List<RoteiroPasso> roteiro = ordem.getRoteiro() == null
                ? List.of()
                : roteiroPassoRepository.buscarPassosComSetorEEtapa(ordem.getRoteiro().getId());
        List<OrdemSetor> linhas = ordemSetorRepository.buscarLinhasDaOrdem(ordemId);
        List<Refugo> refugos = refugoRepository.buscarPorOrdem(ordemId);

        Map<Integer, String> etapaPorSequencia = new HashMap<>();
        roteiro.stream()
                .filter(rp -> rp.getEtapaSetor() != null)
                .forEach(rp -> etapaPorSequencia.put(rp.getSequencia(), rp.getEtapaSetor().getNome()));

        int descartado = refugos.stream()
                .filter(r -> r.getDestino() == DestinoRefugo.DESCARTE)
                .mapToInt(Refugo::getQtdRefugo)
                .sum();
        int produzidoFinal = calcularProduzidoFinal(roteiro, linhas);
        int meta = ordem.getQuantidade() - descartado;
        double percentual = meta <= 0 ? 0.0 : Math.min(100.0, arredondar(produzidoFinal * 100.0 / meta));

        return new OrdemDetalheDTO(
                ordem.getId(),
                ordem.getNumero(),
                ordem.getProduto().getNome(),
                ordem.getProduto().getCodigo(),
                ordem.getQuantidade(),
                ordem.getDataCriacao(),
                ordem.getObservacao(),
                StatusOrdemResolver.derivar(ordem, linhas),
                produzidoFinal,
                descartado,
                percentual,
                montarRoteiro(roteiro, linhas),
                montarEtapas(linhas, etapaPorSequencia),
                montarHistorico(ordemId, refugos)
        );
    }

    /** Unidades que já passaram pela última etapa do roteiro (inclui as retrabalhadas). */
    private int calcularProduzidoFinal(List<RoteiroPasso> roteiro, List<OrdemSetor> linhas) {
        if (roteiro.isEmpty()) {
            return 0;
        }
        Integer ultimaSequencia = roteiro.getLast().getSequencia();
        return linhas.stream()
                .filter(l -> l.getSequencia().equals(ultimaSequencia))
                .mapToInt(OrdemSetor::getQtdProduzida)
                .sum();
    }





    private List<RoteiroPassoDetalheDTO> montarRoteiro(List<RoteiroPasso> roteiro, List<OrdemSetor> linhas) {
        Map<Integer, List<OrdemSetor>> porSequencia = linhas.stream()
                .collect(Collectors.groupingBy(OrdemSetor::getSequencia));

        List<RoteiroPassoDetalheDTO> passos = new ArrayList<>();
        boolean anterioresConcluidos = true;

        for (RoteiroPasso passo : roteiro) {
            List<OrdemSetor> doPasso = porSequencia.getOrDefault(passo.getSequencia(), List.of());
            boolean todasConcluidas = !doPasso.isEmpty()
                    && doPasso.stream().allMatch(l -> l.getStatus() == EstatusOrdemProducao.CONCLUIDA);

            SituacaoPasso situacao;
            if (doPasso.isEmpty()) {
                situacao = SituacaoPasso.AGUARDANDO;
            } else if (todasConcluidas && anterioresConcluidos) {
                situacao = SituacaoPasso.CONCLUIDO;
            } else {
                situacao = SituacaoPasso.ATUAL;
            }

            anterioresConcluidos = anterioresConcluidos && situacao == SituacaoPasso.CONCLUIDO;
            passos.add(new RoteiroPassoDetalheDTO(rotulo(passo), situacao));
        }
        return passos;
    }

    private String rotulo(RoteiroPasso passo) {
        String setor = passo.getSetor().getNome();
        return passo.getEtapaSetor() == null ? setor : setor + " (" + passo.getEtapaSetor().getNome() + ")";
    }

    private List<EtapaOrdemDetalheDTO> montarEtapas(List<OrdemSetor> linhas, Map<Integer, String> etapaPorSequencia) {
        return linhas.stream()
                .map(l -> new EtapaOrdemDetalheDTO(
                        l.getId(),
                        l.getSetor().getId(),
                        l.getSetor().getNome(),
                        etapaPorSequencia.get(l.getSequencia()),
                        l.getVariante(),
                        l.isOrigemRefugo(),
                        l.getQtdRecebida(),
                        l.getQtdProduzida(),
                        l.getQtdPendente(),
                        percentual(l.getQtdProduzida(), l.getQtdRecebida()),
                        l.getStatus()))
                .toList();
    }

    private List<HistoricoOrdemDTO> montarHistorico(Long ordemId, List<Refugo> refugos) {
        List<HistoricoOrdemDTO> eventos = new ArrayList<>();

        for (Lancamento l : lancamentoRepository.buscarPorOrdem(ordemId)) {
            String etapa = l.getEtapaSetor() != null ? " " + l.getEtapaSetor().getNome() : "";
            String observacao = l.getObservacao() != null && !l.getObservacao().isBlank()
                    ? " · Obs: " + l.getObservacao() : "";
            eventos.add(new HistoricoOrdemDTO(
                    TipoEventoOrdem.PRODUCAO,
                    l.getOrdemSetor().getSetor().getNome() + etapa + " — " + l.getQtdProduzida() + " unid. produzidas",
                    l.getMaquina().getNome() + " · " + l.getOperador().getNome() + observacao,
                    l.getDataHora()));
        }

        for (Refugo r : refugos) {
            String destino = r.getDestino() == DestinoRefugo.RETRABALHO ? "Retrabalho" : "Descarte";
            eventos.add(new HistoricoOrdemDTO(
                    TipoEventoOrdem.REFUGO,
                    "Refugo — " + r.getQtdRefugo() + " unid. · Destino: " + destino,
                    "Detectado em " + r.getOrdemSetor().getSetor().getNome()
                            + " · Origem: " + r.getSetorOrigem().getNome()
                            + " · Motivo: " + r.getMotivo()
                            + " · " + r.getOperador().getNome(),
                    r.getDataHora()));
        }

        for (Retrabalho rt : retrabalhoRepository.buscarPorOrdem(ordemId)) {
            String maquina = rt.getMaquina() != null ? rt.getMaquina().getNome() : "sem máquina";
            eventos.add(new HistoricoOrdemDTO(
                    TipoEventoOrdem.RETRABALHO,
                    "Retrabalho — " + rt.getQtdRefeita() + " unid. refeitas",
                    rt.getRefugo().getSetorOrigem().getNome() + " · " + maquina + " · " + rt.getOperador().getNome(),
                    rt.getDataHora()));
        }

        eventos.sort(Comparator.comparing(HistoricoOrdemDTO::dataHora,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return eventos;
    }

    private double percentual(Integer produzido, Integer recebido) {
        if (recebido == null || recebido == 0) {
            return 0.0;
        }
        return Math.min(100.0, arredondar(produzido * 100.0 / recebido));
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
