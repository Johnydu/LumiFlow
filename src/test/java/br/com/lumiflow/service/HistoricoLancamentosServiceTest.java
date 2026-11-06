package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.lancamento.HistoricoLancamentoFiltroDTO;
import br.com.lumiflow.dto.lancamento.HistoricoLancamentosDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.entity.Lancamento;
import br.com.lumiflow.entity.Maquina;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.Produto;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.LancamentoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoLancamentosServiceTest {

    private static final LocalDateTime DATA = LocalDateTime.of(2025, 3, 10, 14, 30);

    @Mock
    private LancamentoRepository lancamentoRepository;

    @InjectMocks
    private HistoricoLancamentosService service;

    private final Pageable pageable = PageRequest.of(0, 20);

    private Lancamento lancamento(EtapaSetor etapa, Maquina maquina, Operador operador) {
        Produto produto = new Produto();
        produto.setCodigo("P-01");
        produto.setNome("PORTA");
        OrdemProducao ordem = new OrdemProducao();
        ordem.setId(10L);
        ordem.setNumero("OP-0010");
        ordem.setProduto(produto);
        Setor setor = new Setor();
        setor.setNome("CORTE");
        OrdemSetor os = new OrdemSetor();
        os.setOrdemProducao(ordem);
        os.setSetor(setor);
        os.setSequencia(20);
        os.setVariante("LOTE A");
        Lancamento l = new Lancamento();
        l.setOrdemSetor(os);
        l.setEtapaSetor(etapa);
        l.setMaquina(maquina);
        l.setOperador(operador);
        l.setQtdProduzida(7);
        l.setObservacao("OK");
        l.setDataHora(DATA);
        return l;
    }

    private EtapaSetor etapa(String nome) {
        EtapaSetor e = new EtapaSetor();
        e.setNome(nome);
        return e;
    }

    private Maquina maquina(String nome) {
        Maquina m = new Maquina();
        m.setNome(nome);
        return m;
    }

    private Operador operador(String nome) {
        Operador o = new Operador();
        o.setNome(nome);
        return o;
    }

    @Test
    @DisplayName("Mapeia o lançamento completo para o DTO de histórico")
    void mapeiaLancamentoCompleto() {
        Page<Lancamento> pagina = new PageImpl<>(List.of(lancamento(etapa("1ª DOBRA"), maquina("DOBRADEIRA"), operador("ANA"))));
        when(lancamentoRepository.buscarComFiltros(1L, 2L, 3L, pageable)).thenReturn(pagina);

        Page<HistoricoLancamentosDTO> resultado = service.listarComFiltros(
                new HistoricoLancamentoFiltroDTO(1L, 2L, 3L), pageable);

        assertThat(resultado.getContent()).containsExactly(new HistoricoLancamentosDTO(
                10L, "OP-0010", "P-01", "PORTA", "CORTE", "1ª DOBRA", "LOTE A",
                "DOBRADEIRA", 7, "OK", "ANA", DATA));
    }

    @Test
    @DisplayName("Usa a sequência como nome da etapa e traço para máquina/operador ausentes")
    void usaFallbacks() {
        Page<Lancamento> pagina = new PageImpl<>(List.of(lancamento(null, null, null)));
        when(lancamentoRepository.buscarComFiltros(null, null, null, pageable)).thenReturn(pagina);

        HistoricoLancamentosDTO dto = service.listarComFiltros(
                new HistoricoLancamentoFiltroDTO(null, null, null), pageable).getContent().get(0);

        assertThat(dto.etapa()).isEqualTo("20");
        assertThat(dto.maquina()).isEqualTo("—");
        assertThat(dto.operador()).isEqualTo("—");
    }

    @Test
    @DisplayName("Filtro nulo é tratado como sem filtros")
    void filtroNulo() {
        when(lancamentoRepository.buscarComFiltros(null, null, null, pageable)).thenReturn(Page.empty(pageable));

        Page<HistoricoLancamentosDTO> resultado = service.listarComFiltros(null, pageable);

        assertThat(resultado).isEmpty();
        verify(lancamentoRepository).buscarComFiltros(null, null, null, pageable);
    }

    @Test
    @DisplayName("Preserva os metadados de paginação do repositório")
    void preservaPaginacao() {
        Page<Lancamento> pagina = new PageImpl<>(List.of(lancamento(null, null, null)), PageRequest.of(2, 1), 5);
        when(lancamentoRepository.buscarComFiltros(null, null, null, PageRequest.of(2, 1))).thenReturn(pagina);

        Page<HistoricoLancamentosDTO> resultado = service.listarComFiltros(null, PageRequest.of(2, 1));

        assertThat(resultado.getTotalElements()).isEqualTo(5);
        assertThat(resultado.getNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Erro de banco vira BusinessException de validação")
    void erroDeBanco() {
        when(lancamentoRepository.buscarComFiltros(null, null, null, pageable))
                .thenThrow(new DataRetrievalFailureException("falha"));

        assertThatThrownBy(() -> service.listarComFiltros(null, pageable))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }

    @Test
    @DisplayName("Erro inesperado no mapeamento vira BusinessException de validação")
    void erroInesperado() {
        Lancamento semOrdemSetor = new Lancamento();
        when(lancamentoRepository.buscarComFiltros(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(semOrdemSetor)));

        assertThatThrownBy(() -> service.listarComFiltros(null, pageable))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }
}
