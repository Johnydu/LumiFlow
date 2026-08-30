package br.com.lumiflow.service.producao;

import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConclusaoOrdemService {

    private final OrdemSetorRepository ordemSetorRepository;
    private final OrdemProducaoRepository ordemProducaoRepository;

    public void verificarEFinalizar(OrdemProducao ordem) {
        List<OrdemSetor> etapas = ordemSetorRepository
                .findByOrdemProducaoIdOrderBySequenciaAsc(ordem.getId());

        OrdemSetor ultimaEtapa = etapas.get(etapas.size() - 1);

        boolean ultimaEtapaCompleta = ultimaEtapa.getQtdPendente() == 0
                && ultimaEtapa.getQtdProduzida() > 0;

        if (ultimaEtapaCompleta && !EstatusOrdemProducao.CONCLUIDA.equals(ordem.getStatus())) {
            ordem.setStatus(EstatusOrdemProducao.CONCLUIDA);
            ordemProducaoRepository.save(ordem);
        }
    }
}