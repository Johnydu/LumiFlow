package br.com.lumiflow.service;

import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusOrdemResolverTest {

    private OrdemProducao ordem(EstatusOrdemProducao status) {
        OrdemProducao o = new OrdemProducao();
        o.setStatus(status);
        return o;
    }

    private OrdemSetor linha(EstatusOrdemProducao status) {
        OrdemSetor l = new OrdemSetor();
        l.setStatus(status);
        return l;
    }

    @Test
    @DisplayName("Ordem concluída permanece concluída independentemente das linhas")
    void ordemConcluidaPrevalece() {
        var status = StatusOrdemResolver.derivar(ordem(EstatusOrdemProducao.CONCLUIDA),
                List.of(linha(EstatusOrdemProducao.EM_ANDAMENTO)));

        assertThat(status).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
    }

    @Test
    @DisplayName("Qualquer linha em andamento torna a ordem EM_ANDAMENTO, mesmo com outras liberadas")
    void emAndamentoTemPrioridadeSobreLiberada() {
        var status = StatusOrdemResolver.derivar(ordem(EstatusOrdemProducao.DISPONIVEL),
                List.of(linha(EstatusOrdemProducao.LIBERADA), linha(EstatusOrdemProducao.EM_ANDAMENTO),
                        linha(EstatusOrdemProducao.CONCLUIDA)));

        assertThat(status).isEqualTo(EstatusOrdemProducao.EM_ANDAMENTO);
    }

    @Test
    @DisplayName("Sem linhas em andamento, uma linha liberada torna a ordem LIBERADA")
    void liberada() {
        var status = StatusOrdemResolver.derivar(ordem(EstatusOrdemProducao.DISPONIVEL),
                List.of(linha(EstatusOrdemProducao.DISPONIVEL), linha(EstatusOrdemProducao.LIBERADA)));

        assertThat(status).isEqualTo(EstatusOrdemProducao.LIBERADA);
    }

    @Test
    @DisplayName("Sem linhas ativas a ordem é DISPONIVEL")
    void disponivel() {
        assertThat(StatusOrdemResolver.derivar(ordem(EstatusOrdemProducao.DISPONIVEL),
                List.of(linha(EstatusOrdemProducao.DISPONIVEL)))).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
        assertThat(StatusOrdemResolver.derivar(ordem(EstatusOrdemProducao.DISPONIVEL), List.of()))
                .isEqualTo(EstatusOrdemProducao.DISPONIVEL);
    }

    @Test
    @DisplayName("Todas as linhas concluídas: a ordem é CONCLUIDA mesmo com o status gravado defasado")
    void linhasConcluidasConcluemAOrdem() {
        var status = StatusOrdemResolver.derivar(ordem(EstatusOrdemProducao.DISPONIVEL),
                List.of(linha(EstatusOrdemProducao.CONCLUIDA)));

        assertThat(status).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
    }

    @Test
    @DisplayName("Classe utilitária não pode ser instanciada")
    void construtorPrivado() throws NoSuchMethodException {
        Constructor<StatusOrdemResolver> c = StatusOrdemResolver.class.getDeclaredConstructor();
        c.setAccessible(true);

        assertThatThrownBy(c::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
