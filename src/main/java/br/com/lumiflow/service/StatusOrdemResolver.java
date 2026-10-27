package br.com.lumiflow.service;

import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;

import java.util.Collection;

/**
 * O status gravado na ordem só assume DISPONIVEL ou CONCLUIDA. Para exibição, o status é
 * derivado das linhas por setor. Regra única, usada pelo dashboard e pelos detalhes da ordem.
 */
public final class StatusOrdemResolver {

    private StatusOrdemResolver() {
        throw new IllegalStateException("Classe utilitária");
    }

    public static EstatusOrdemProducao derivar(OrdemProducao ordem, Collection<OrdemSetor> linhas) {
        if (ordem.getStatus() == EstatusOrdemProducao.CONCLUIDA) {
            return EstatusOrdemProducao.CONCLUIDA;
        }
        if (linhas.stream().anyMatch(l -> l.getStatus() == EstatusOrdemProducao.EM_ANDAMENTO)) {
            return EstatusOrdemProducao.EM_ANDAMENTO;
        }
        if (linhas.stream().anyMatch(l -> l.getStatus() == EstatusOrdemProducao.LIBERADA)) {
            return EstatusOrdemProducao.LIBERADA;
        }
        return EstatusOrdemProducao.DISPONIVEL;
    }
}