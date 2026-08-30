package br.com.lumiflow.service.producao;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.RoteiroProduto;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.OrdemSetorRepository;
import br.com.lumiflow.repository.RoteiroProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AvancoRoteiroService {

    private final OrdemSetorRepository ordemSetorRepository;
    private final RoteiroProdutoRepository roteiroProdutoRepository;

    /**
     * @return true se havia um próximo setor e a quantidade foi movida para ele;
     *         false se esta era a última etapa do roteiro (produto finalizado).
     */
    public boolean avancar(OrdemSetor etapaAtual, int quantidadeBoa) {
        Long produtoId = etapaAtual.getOrdemProducao().getProduto().getId();
        int proximaSequencia = etapaAtual.getSequencia() + 1;

        Optional<RoteiroProduto> proximoPasso =
                roteiroProdutoRepository.findByProdutoIdAndSequencia(produtoId, proximaSequencia);

        if (proximoPasso.isEmpty()) {
            return false; // era a última etapa do roteiro
        }

        OrdemSetor proximaEtapa = ordemSetorRepository
                .findByOrdemProducaoIdAndSequencia(etapaAtual.getOrdemProducao().getId(), proximaSequencia)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ROTEIRO_ETAPA_NAO_CRIADA));

        proximaEtapa.setQtdRecebida(proximaEtapa.getQtdRecebida() + quantidadeBoa);
        proximaEtapa.setQtdPendente(proximaEtapa.getQtdPendente() + quantidadeBoa);

        if (EstatusOrdemProducao.DISPONIVEL.equals(proximaEtapa.getStatus())
                && proximaEtapa.getInicio() == null) {
            proximaEtapa.setInicio(OffsetDateTime.now().toLocalDateTime());
        }

        ordemSetorRepository.save(proximaEtapa);
        return true;
    }
}