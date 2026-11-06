package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.EtapaOrdemDetalheDTO;
import br.com.lumiflow.dto.ordens.HistoricoOrdemDTO;
import br.com.lumiflow.dto.ordens.OrdemDetalheDTO;
import br.com.lumiflow.dto.ordens.RoteiroPassoDetalheDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.entity.Lancamento;
import br.com.lumiflow.entity.Maquina;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.Produto;
import br.com.lumiflow.entity.Refugo;
import br.com.lumiflow.entity.Retrabalho;
import br.com.lumiflow.entity.Roteiro;
import br.com.lumiflow.entity.RoteiroPasso;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.entity.enums.SituacaoPasso;
import br.com.lumiflow.entity.enums.TipoEventoOrdem;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.LancamentoRepository;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import br.com.lumiflow.repository.RefugoRepository;
import br.com.lumiflow.repository.RetrabalhoRepository;
import br.com.lumiflow.repository.RoteiroPassoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemDetalheServiceTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2025, 1, 1, 10, 0);
    private static final LocalDateTime T2 = LocalDateTime.of(2025, 1, 1, 11, 0);
    private static final LocalDateTime T3 = LocalDateTime.of(2025, 1, 1, 12, 0);

    @Mock
    private OrdemProducaoRepository ordemProducaoRepository;
    @Mock
    private OrdemSetorRepository ordemSetorRepository;
    @Mock
    private RoteiroPassoRepository roteiroPassoRepository;
    @Mock
    private LancamentoRepository lancamentoRepository;
    @Mock
    private RefugoRepository refugoRepository;
    @Mock
    private RetrabalhoRepository retrabalhoRepository;

    @InjectMocks
    private OrdemDetalheService service;

    private final Setor corte = setor(10L, "CORTE");
    private final Setor dobra = setor(11L, "DOBRA");
    private final Setor solda = setor(12L, "SOLDA");

    private static Setor setor(Long id, String nome) {
        Setor s = new Setor();
        s.setId(id);
        s.setNome(nome);
        return s;
    }

    private OrdemProducao ordem(int quantidade, Roteiro roteiro, EstatusOrdemProducao status) {
        Produto produto = new Produto();
        produto.setNome("PORTA");
        produto.setCodigo("P-01");
        OrdemProducao o = new OrdemProducao();
        o.setId(1L);
        o.setNumero("OP-0001");
        o.setQuantidade(quantidade);
        o.setObservacao("obs");
        o.setDataCriacao(T1);
        o.setStatus(status);
        o.setProduto(produto);
        o.setRoteiro(roteiro);
        return o;
    }

    private Roteiro roteiro() {
        Roteiro r = new Roteiro();
        r.setId(5L);
        return r;
    }

    private RoteiroPasso passo(int sequencia, Setor setor, EtapaSetor etapa) {
        RoteiroPasso p = new RoteiroPasso();
        p.setSequencia(sequencia);
        p.setSetor(setor);
        p.setEtapaSetor(etapa);
        return p;
    }

    private EtapaSetor etapa(String nome) {
        EtapaSetor e = new EtapaSetor();
        e.setNome(nome);
        return e;
    }

    private OrdemSetor linha(Long id, Setor setor, int sequencia, int recebida, int produzida,
                             EstatusOrdemProducao status) {
        OrdemSetor os = new OrdemSetor();
        os.setId(id);
        os.setSetor(setor);
        os.setSequencia(sequencia);
        os.setQtdRecebida(recebida);
        os.setQtdProduzida(produzida);
        os.setQtdPendente(recebida - produzida);
        os.setStatus(status);
        return os;
    }

    private Operador operador(String nome) {
        Operador o = new Operador();
        o.setNome(nome);
        return o;
    }

    private Maquina maquina(String nome) {
        Maquina m = new Maquina();
        m.setNome(nome);
        return m;
    }

    private Refugo refugo(OrdemSetor onde, Setor origem, int qtd, DestinoRefugo destino, LocalDateTime quando) {
        Refugo r = new Refugo();
        r.setOrdemSetor(onde);
        r.setSetorOrigem(origem);
        r.setQtdRefugo(qtd);
        r.setDestino(destino);
        r.setMotivo("TRINCA");
        r.setOperador(operador("BRUNO"));
        r.setDataHora(quando);
        return r;
    }

    private void stubOrdem(OrdemProducao ordem) {
        when(ordemProducaoRepository.buscarComProduto(1L)).thenReturn(Optional.of(ordem));
    }

    @Test
    @DisplayName("Id nulo ou ordem inexistente resultam em ordem não encontrada")
    void naoEncontrada() {
        when(ordemProducaoRepository.buscarComProduto(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarDetalhe(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_ORDER_NOTFOUND);
        assertThatThrownBy(() -> service.buscarDetalhe(9L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_ORDER_NOTFOUND);
        verifyNoInteractions(ordemSetorRepository, refugoRepository);
    }

    @Test
    @DisplayName("Calcula produzido final (última etapa), descarte, percentual sobre a meta e status derivado")
    void calculosPrincipais() {
        Roteiro roteiro = roteiro();
        OrdemProducao ordem = ordem(100, roteiro, EstatusOrdemProducao.DISPONIVEL);
        OrdemSetor l1 = linha(1L, corte, 10, 100, 100, EstatusOrdemProducao.CONCLUIDA);
        OrdemSetor l2 = linha(2L, dobra, 20, 90, 45, EstatusOrdemProducao.EM_ANDAMENTO);
        OrdemSetor l3 = linha(3L, solda, 30, 45, 45, EstatusOrdemProducao.CONCLUIDA);
        stubOrdem(ordem);
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(
                passo(10, corte, null), passo(20, dobra, etapa("1ª DOBRA")), passo(30, solda, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L)).thenReturn(List.of(l1, l2, l3));
        when(refugoRepository.buscarPorOrdem(1L)).thenReturn(List.of(
                refugo(l2, corte, 10, DestinoRefugo.DESCARTE, T1),
                refugo(l2, corte, 5, DestinoRefugo.RETRABALHO, T1)));

        OrdemDetalheDTO dto = service.buscarDetalhe(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.numero()).isEqualTo("OP-0001");
        assertThat(dto.produtoNome()).isEqualTo("PORTA");
        assertThat(dto.produtoCodigo()).isEqualTo("P-01");
        assertThat(dto.quantidade()).isEqualTo(100);
        assertThat(dto.dataCriacao()).isEqualTo(T1);
        assertThat(dto.observacao()).isEqualTo("obs");
        assertThat(dto.status()).isEqualTo(EstatusOrdemProducao.EM_ANDAMENTO);
        assertThat(dto.produzidoFinal()).isEqualTo(45);
        assertThat(dto.descartado()).as("somente refugos com destino DESCARTE").isEqualTo(10);
        assertThat(dto.percentual()).as("45 / (100 - 10)").isEqualTo(50.0);
    }

    @Test
    @DisplayName("Roteiro marca CONCLUIDO, ATUAL e AGUARDANDO conforme o andamento das linhas")
    void situacaoDosPassos() {
        Roteiro roteiro = roteiro();
        stubOrdem(ordem(100, roteiro, EstatusOrdemProducao.DISPONIVEL));
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(
                passo(10, corte, null), passo(20, dobra, etapa("1ª DOBRA")), passo(30, solda, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L)).thenReturn(List.of(
                linha(1L, corte, 10, 100, 100, EstatusOrdemProducao.CONCLUIDA),
                linha(2L, dobra, 20, 100, 10, EstatusOrdemProducao.EM_ANDAMENTO)));

        List<RoteiroPassoDetalheDTO> passos = service.buscarDetalhe(1L).roteiro();

        assertThat(passos).containsExactly(
                new RoteiroPassoDetalheDTO("CORTE", SituacaoPasso.CONCLUIDO),
                new RoteiroPassoDetalheDTO("DOBRA (1ª DOBRA)", SituacaoPasso.ATUAL),
                new RoteiroPassoDetalheDTO("SOLDA", SituacaoPasso.AGUARDANDO));
    }

    @Test
    @DisplayName("Passo concluído depois de um passo pendente continua ATUAL (exige todos os anteriores concluídos)")
    void passoConcluidoAposPendenteNaoEhConcluido() {
        Roteiro roteiro = roteiro();
        stubOrdem(ordem(100, roteiro, EstatusOrdemProducao.DISPONIVEL));
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(
                passo(10, corte, null), passo(20, dobra, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L)).thenReturn(List.of(
                linha(1L, corte, 10, 100, 50, EstatusOrdemProducao.EM_ANDAMENTO),
                linha(2L, dobra, 20, 50, 50, EstatusOrdemProducao.CONCLUIDA)));

        assertThat(service.buscarDetalhe(1L).roteiro()).extracting(RoteiroPassoDetalheDTO::situacao)
                .containsExactly(SituacaoPasso.ATUAL, SituacaoPasso.ATUAL);
    }

    @Test
    @DisplayName("Passo com variantes só é CONCLUIDO quando todas as linhas do passo estão concluídas")
    void passoComVariantes() {
        Roteiro roteiro = roteiro();
        stubOrdem(ordem(100, roteiro, EstatusOrdemProducao.DISPONIVEL));
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(passo(10, corte, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L)).thenReturn(List.of(
                linha(1L, corte, 10, 50, 50, EstatusOrdemProducao.CONCLUIDA),
                linha(2L, corte, 10, 50, 0, EstatusOrdemProducao.DISPONIVEL)));

        assertThat(service.buscarDetalhe(1L).roteiro()).extracting(RoteiroPassoDetalheDTO::situacao)
                .containsExactly(SituacaoPasso.ATUAL);
    }

    @Test
    @DisplayName("Etapas por linha trazem nome da etapa do roteiro, quantidades e percentual limitado a 100")
    void etapasPorLinha() {
        Roteiro roteiro = roteiro();
        OrdemSetor normal = linha(1L, dobra, 20, 90, 45, EstatusOrdemProducao.EM_ANDAMENTO);
        normal.setVariante("LOTE A");
        OrdemSetor refugoLinha = linha(2L, corte, 10, 0, 0, EstatusOrdemProducao.DISPONIVEL);
        refugoLinha.setOrigemRefugo(true);
        OrdemSetor estourada = linha(3L, solda, 30, 10, 15, EstatusOrdemProducao.CONCLUIDA);
        stubOrdem(ordem(100, roteiro, EstatusOrdemProducao.DISPONIVEL));
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(
                passo(10, corte, null), passo(20, dobra, etapa("1ª DOBRA")), passo(30, solda, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L)).thenReturn(List.of(normal, refugoLinha, estourada));

        List<EtapaOrdemDetalheDTO> etapas = service.buscarDetalhe(1L).etapas();

        assertThat(etapas).containsExactly(
                new EtapaOrdemDetalheDTO(1L, 11L, "DOBRA", "1ª DOBRA", "LOTE A", false, 90, 45, 45, 50.0,
                        EstatusOrdemProducao.EM_ANDAMENTO),
                new EtapaOrdemDetalheDTO(2L, 10L, "CORTE", null, null, true, 0, 0, 0, 0.0,
                        EstatusOrdemProducao.DISPONIVEL),
                new EtapaOrdemDetalheDTO(3L, 12L, "SOLDA", null, null, false, 10, 15, -5, 100.0,
                        EstatusOrdemProducao.CONCLUIDA));
    }

    @Test
    @DisplayName("Percentual geral é arredondado a uma casa decimal")
    void percentualArredondado() {
        Roteiro roteiro = roteiro();
        stubOrdem(ordem(3, roteiro, EstatusOrdemProducao.DISPONIVEL));
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(passo(10, corte, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L))
                .thenReturn(List.of(linha(1L, corte, 10, 3, 1, EstatusOrdemProducao.EM_ANDAMENTO)));

        assertThat(service.buscarDetalhe(1L).percentual()).isEqualTo(33.3);
    }

    @Test
    @DisplayName("Percentual geral nunca passa de 100")
    void percentualLimitadoACem() {
        Roteiro roteiro = roteiro();
        stubOrdem(ordem(10, roteiro, EstatusOrdemProducao.CONCLUIDA));
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(passo(10, corte, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L))
                .thenReturn(List.of(linha(1L, corte, 10, 10, 15, EstatusOrdemProducao.CONCLUIDA)));

        OrdemDetalheDTO dto = service.buscarDetalhe(1L);

        assertThat(dto.percentual()).isEqualTo(100.0);
        assertThat(dto.status()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
    }

    @Test
    @DisplayName("Meta zero ou negativa (tudo descartado) resulta em percentual zero, sem divisão inválida")
    void metaZeroOuNegativa() {
        Roteiro roteiro = roteiro();
        OrdemSetor l = linha(1L, corte, 10, 10, 0, EstatusOrdemProducao.DISPONIVEL);
        stubOrdem(ordem(10, roteiro, EstatusOrdemProducao.DISPONIVEL));
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(passo(10, corte, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L)).thenReturn(List.of(l));
        when(refugoRepository.buscarPorOrdem(1L)).thenReturn(List.of(refugo(l, corte, 10, DestinoRefugo.DESCARTE, T1)));

        OrdemDetalheDTO dto = service.buscarDetalhe(1L);

        assertThat(dto.descartado()).isEqualTo(10);
        assertThat(dto.percentual()).isZero();
    }

    @Test
    @DisplayName("Ordem sem roteiro não consulta passos e tem produzido final zero")
    void ordemSemRoteiro() {
        stubOrdem(ordem(10, null, EstatusOrdemProducao.DISPONIVEL));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L)).thenReturn(List.of());

        OrdemDetalheDTO dto = service.buscarDetalhe(1L);

        assertThat(dto.roteiro()).isEmpty();
        assertThat(dto.etapas()).isEmpty();
        assertThat(dto.produzidoFinal()).isZero();
        assertThat(dto.percentual()).isZero();
        assertThat(dto.status()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
        verifyNoInteractions(roteiroPassoRepository);
    }

    @Test
    @DisplayName("Histórico reúne produção, refugo e retrabalho em ordem cronológica decrescente, com nulos por último")
    void historicoOrdenadoEFormatado() {
        Roteiro roteiro = roteiro();
        OrdemSetor l1 = linha(1L, dobra, 20, 100, 45, EstatusOrdemProducao.EM_ANDAMENTO);

        Lancamento comEtapa = new Lancamento();
        comEtapa.setOrdemSetor(l1);
        comEtapa.setEtapaSetor(etapa("1ª DOBRA"));
        comEtapa.setQtdProduzida(45);
        comEtapa.setMaquina(maquina("DOBRADEIRA"));
        comEtapa.setOperador(operador("ANA"));
        comEtapa.setObservacao("ok");
        comEtapa.setDataHora(T1);

        Lancamento semDataSemObs = new Lancamento();
        semDataSemObs.setOrdemSetor(l1);
        semDataSemObs.setQtdProduzida(1);
        semDataSemObs.setMaquina(maquina("DOBRADEIRA"));
        semDataSemObs.setOperador(operador("ANA"));
        semDataSemObs.setObservacao("   ");
        semDataSemObs.setDataHora(null);

        Refugo refugo = refugo(l1, corte, 5, DestinoRefugo.RETRABALHO, T2);

        Retrabalho retrabalho = new Retrabalho();
        retrabalho.setRefugo(refugo);
        retrabalho.setQtdRefeita(3);
        retrabalho.setOperador(operador("ANA"));
        retrabalho.setMaquina(null);
        retrabalho.setDataHora(T3);

        stubOrdem(ordem(100, roteiro, EstatusOrdemProducao.DISPONIVEL));
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(passo(20, dobra, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L)).thenReturn(List.of(l1));
        when(refugoRepository.buscarPorOrdem(1L)).thenReturn(List.of(refugo));
        when(lancamentoRepository.buscarPorOrdem(1L)).thenReturn(List.of(semDataSemObs, comEtapa));
        when(retrabalhoRepository.buscarPorOrdem(1L)).thenReturn(List.of(retrabalho));

        List<HistoricoOrdemDTO> historico = service.buscarDetalhe(1L).historico();

        assertThat(historico).containsExactly(
                new HistoricoOrdemDTO(TipoEventoOrdem.RETRABALHO, "Retrabalho — 3 unid. refeitas",
                        "CORTE · sem máquina · ANA", T3),
                new HistoricoOrdemDTO(TipoEventoOrdem.REFUGO, "Refugo — 5 unid. · Destino: Retrabalho",
                        "Detectado em DOBRA · Origem: CORTE · Motivo: TRINCA · BRUNO", T2),
                new HistoricoOrdemDTO(TipoEventoOrdem.PRODUCAO, "DOBRA 1ª DOBRA — 45 unid. produzidas",
                        "DOBRADEIRA · ANA · Obs: ok", T1),
                new HistoricoOrdemDTO(TipoEventoOrdem.PRODUCAO, "DOBRA — 1 unid. produzidas",
                        "DOBRADEIRA · ANA", null));
    }

    @Test
    @DisplayName("Refugo com destino DESCARTE aparece com o rótulo Descarte")
    void historicoRefugoDescarte() {
        Roteiro roteiro = roteiro();
        OrdemSetor l1 = linha(1L, dobra, 20, 100, 45, EstatusOrdemProducao.EM_ANDAMENTO);
        stubOrdem(ordem(100, roteiro, EstatusOrdemProducao.DISPONIVEL));
        when(roteiroPassoRepository.buscarPassosComSetorEEtapa(5L)).thenReturn(List.of(passo(20, dobra, null)));
        when(ordemSetorRepository.buscarLinhasDaOrdem(1L)).thenReturn(List.of(l1));
        when(refugoRepository.buscarPorOrdem(1L)).thenReturn(List.of(refugo(l1, dobra, 2, DestinoRefugo.DESCARTE, T1)));

        assertThat(service.buscarDetalhe(1L).historico()).extracting(HistoricoOrdemDTO::titulo)
                .containsExactly("Refugo — 2 unid. · Destino: Descarte");
    }
}
