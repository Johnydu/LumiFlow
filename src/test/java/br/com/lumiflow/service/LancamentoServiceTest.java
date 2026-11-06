package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.lancamento.LancamentoProducaoDTO;
import br.com.lumiflow.dto.lancamento.RefugoDTO;
import br.com.lumiflow.dto.lancamento.RetrabalhoDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.entity.Lancamento;
import br.com.lumiflow.entity.Maquina;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.Refugo;
import br.com.lumiflow.entity.Retrabalho;
import br.com.lumiflow.entity.Roteiro;
import br.com.lumiflow.entity.RoteiroPasso;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.LancamentoRepository;
import br.com.lumiflow.repository.MaquinaRepository;
import br.com.lumiflow.repository.OperadorRepository;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import br.com.lumiflow.repository.RefugoRepository;
import br.com.lumiflow.repository.RetrabalhoRepository;
import br.com.lumiflow.repository.RoteiroPassoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    private static final Long ORDEM_ID = 10L;
    private static final Long ETAPA_ID = 100L;

    @Mock
    private OrdemSetorRepository ordemSetorRepository;
    @Mock
    private OrdemProducaoRepository ordemProducaoRepository;
    @Mock
    private LancamentoRepository lancamentoRepository;
    @Mock
    private RefugoRepository refugoRepository;
    @Mock
    private RetrabalhoRepository retrabalhoRepository;
    @Mock
    private MaquinaRepository maquinaRepository;
    @Mock
    private RoteiroPassoRepository roteiroPassoRepository;
    @Mock
    private SetorService setorService;
    @Mock
    private OperadorRepository operadorRepository;
    @Mock
    private OperadorService operadorService;

    @InjectMocks
    private LancamentoService service;

    // ---------- fixtures ----------

    private final Setor setor5 = setor(5L, "CORTE");
    private final Setor setor6 = setor(6L, "DOBRA");
    private final Roteiro roteiro3 = roteiro(3L);
    private final OrdemProducao ordem10 = ordem(ORDEM_ID, roteiro3);

    private static void assertChave(Runnable acao, String chave) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", chave);
    }

    private static Setor setor(Long id, String nome) {
        Setor s = new Setor();
        s.setId(id);
        s.setNome(nome);
        return s;
    }

    private static Roteiro roteiro(Long id) {
        Roteiro r = new Roteiro();
        r.setId(id);
        return r;
    }

    private static OrdemProducao ordem(Long id, Roteiro roteiro) {
        OrdemProducao o = new OrdemProducao();
        o.setId(id);
        o.setRoteiro(roteiro);
        o.setStatus(EstatusOrdemProducao.DISPONIVEL);
        return o;
    }

    private OrdemSetor etapa(Long id, Setor setor, int sequencia, int recebida, int produzida,
                             EstatusOrdemProducao status) {
        OrdemSetor os = new OrdemSetor();
        os.setId(id);
        os.setOrdemProducao(ordem10);
        os.setSetor(setor);
        os.setSequencia(sequencia);
        os.setQtdRecebida(recebida);
        os.setQtdProduzida(produzida);
        os.setQtdPendente(recebida - produzida);
        os.setStatus(status);
        os.setVariante("A");
        return os;
    }

    /** Etapa ativa padrão: setor CORTE, sequência 20, 10 recebidas, 0 produzidas. */
    private OrdemSetor etapaAtiva(EstatusOrdemProducao status) {
        return etapa(ETAPA_ID, setor5, 20, 10, 0, status);
    }

    private RoteiroPasso passo(int sequencia, Setor setor, EtapaSetor etapa) {
        RoteiroPasso p = new RoteiroPasso();
        p.setRoteiro(roteiro3);
        p.setSequencia(sequencia);
        p.setSetor(setor);
        p.setEtapaSetor(etapa);
        return p;
    }

    private EtapaSetor etapaSetor(Long id, String nome, Setor setor) {
        EtapaSetor e = new EtapaSetor();
        e.setId(id);
        e.setNome(nome);
        e.setSetor(setor);
        return e;
    }

    private Maquina maquina(Long id, Setor setor) {
        Maquina m = new Maquina();
        m.setId(id);
        m.setNome("MAQ " + id);
        m.setSetor(setor);
        return m;
    }

    private Operador operador(Long id, Setor setorPadrao) {
        Operador o = new Operador();
        o.setId(id);
        o.setNome("OP " + id);
        o.setSetorPadrao(setorPadrao);
        return o;
    }

    // ---------- consultas ----------

    @Nested
    @DisplayName("buscarEtapaAtiva")
    class BuscarEtapaAtiva {

        @Test
        @DisplayName("Ids nulos resultam em ordem indisponível sem consultar o banco")
        void idsNulos() {
            assertChave(() -> service.buscarEtapaAtiva(null, ETAPA_ID), AppMessages.ERROR_ORDER_NOT_AVAILABLE);
            assertChave(() -> service.buscarEtapaAtiva(ORDEM_ID, null), AppMessages.ERROR_ORDER_NOT_AVAILABLE);

            verifyNoInteractions(ordemSetorRepository);
        }

        @Test
        @DisplayName("Etapa inexistente resulta em ordem indisponível")
        void inexistente() {
            when(ordemSetorRepository.findById(ETAPA_ID)).thenReturn(Optional.empty());

            assertChave(() -> service.buscarEtapaAtiva(ORDEM_ID, ETAPA_ID), AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("Etapa pertencente a outra ordem é rejeitada")
        void deOutraOrdem() {
            when(ordemSetorRepository.findById(ETAPA_ID)).thenReturn(Optional.of(etapaAtiva(EstatusOrdemProducao.LIBERADA)));

            assertChave(() -> service.buscarEtapaAtiva(999L, ETAPA_ID), AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        @ParameterizedTest
        @EnumSource(value = EstatusOrdemProducao.class, names = {"DISPONIVEL", "CONCLUIDA"})
        @DisplayName("Etapa DISPONIVEL ou CONCLUIDA não aceita lançamentos")
        void statusInvalido(EstatusOrdemProducao status) {
            when(ordemSetorRepository.findById(ETAPA_ID)).thenReturn(Optional.of(etapaAtiva(status)));

            assertChave(() -> service.buscarEtapaAtiva(ORDEM_ID, ETAPA_ID), AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        @ParameterizedTest
        @EnumSource(value = EstatusOrdemProducao.class, names = {"LIBERADA", "EM_ANDAMENTO"})
        @DisplayName("Etapa LIBERADA ou EM_ANDAMENTO é retornada")
        void statusValido(EstatusOrdemProducao status) {
            OrdemSetor os = etapaAtiva(status);
            when(ordemSetorRepository.findById(ETAPA_ID)).thenReturn(Optional.of(os));

            assertThat(service.buscarEtapaAtiva(ORDEM_ID, ETAPA_ID)).isSameAs(os);
        }
    }

    @Nested
    @DisplayName("listagens de apoio")
    class Listagens {

        @Test
        @DisplayName("listarMaquinasDoSetor e listarOperadoresDoSetor delegam aos repositórios ordenados")
        void delegam() {
            List<Maquina> maquinas = List.of(maquina(1L, setor5));
            List<Operador> operadores = List.of(operador(1L, null));
            when(maquinaRepository.findBySetorIdOrderByNomeAsc(5L)).thenReturn(maquinas);
            when(operadorRepository.findBySetorPadraoIdOrSetorPadraoIsNullOrderByNomeAsc(5L)).thenReturn(operadores);

            assertThat(service.listarMaquinasDoSetor(5L)).isSameAs(maquinas);
            assertThat(service.listarOperadoresDoSetor(5L)).isSameAs(operadores);
        }

        @Test
        @DisplayName("listarRefugosParaRetrabalho consulta apenas refugos com destino RETRABALHO")
        void refugosParaRetrabalho() {
            List<Refugo> refugos = List.of(new Refugo());
            when(refugoRepository.findByOrdemSetor_OrdemProducao_IdAndDestinoOrderByDataHoraDesc(
                    ORDEM_ID, DestinoRefugo.RETRABALHO)).thenReturn(refugos);

            assertThat(service.listarRefugosParaRetrabalho(ORDEM_ID)).isSameAs(refugos);
        }

        @Test
        @DisplayName("listarMaquinasParaRetrabalho busca máquinas dos setores de origem sem repetir setores")
        void maquinasParaRetrabalho() {
            Refugo r1 = refugoComOrigem(setor5);
            Refugo r2 = refugoComOrigem(setor6);
            Refugo r3 = refugoComOrigem(setor5);
            List<Maquina> maquinas = List.of(maquina(1L, setor5));
            when(refugoRepository.findByOrdemSetor_OrdemProducao_IdAndDestinoOrderByDataHoraDesc(
                    ORDEM_ID, DestinoRefugo.RETRABALHO)).thenReturn(List.of(r1, r2, r3));
            when(maquinaRepository.findBySetorIdInOrderByNomeAsc(List.of(5L, 6L))).thenReturn(maquinas);

            assertThat(service.listarMaquinasParaRetrabalho(ORDEM_ID)).isSameAs(maquinas);
        }

        @Test
        @DisplayName("listarMaquinasParaRetrabalho sem refugos retorna vazio sem consultar máquinas")
        void maquinasSemRefugos() {
            when(refugoRepository.findByOrdemSetor_OrdemProducao_IdAndDestinoOrderByDataHoraDesc(
                    ORDEM_ID, DestinoRefugo.RETRABALHO)).thenReturn(List.of());

            assertThat(service.listarMaquinasParaRetrabalho(ORDEM_ID)).isEmpty();

            verifyNoInteractions(maquinaRepository);
        }

        @Test
        @DisplayName("buscarNomeEtapa retorna o nome da etapa do passo ou nulo quando não há etapa/passo")
        void nomeDaEtapa() {
            OrdemSetor os = etapaAtiva(EstatusOrdemProducao.LIBERADA);
            when(roteiroPassoRepository.findByRoteiroIdAndSequencia(3L, 20))
                    .thenReturn(Optional.of(passo(20, setor5, etapaSetor(1L, "SERRA", setor5))))
                    .thenReturn(Optional.of(passo(20, setor5, null)))
                    .thenReturn(Optional.empty());

            assertThat(service.buscarNomeEtapa(os)).isEqualTo("SERRA");
            assertThat(service.buscarNomeEtapa(os)).isNull();
            assertThat(service.buscarNomeEtapa(os)).isNull();
        }

        @Test
        @DisplayName("buscarNomeEtapa de ordem sem roteiro lança produto sem roteiro")
        void nomeDaEtapaSemRoteiro() {
            OrdemSetor os = etapaAtiva(EstatusOrdemProducao.LIBERADA);
            os.getOrdemProducao().setRoteiro(null);

            assertChave(() -> service.buscarNomeEtapa(os), AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE);
        }

        private Refugo refugoComOrigem(Setor origem) {
            Refugo r = new Refugo();
            r.setSetorOrigem(origem);
            return r;
        }
    }

    // ---------- lançar produção ----------

    @Nested
    @DisplayName("lancarProducao")
    class LancarProducao {

        private final Maquina maquina7 = maquina(7L, setor5);
        private final Operador operador8 = operador(8L, null);

        private LancamentoProducaoDTO dto(int quantidade) {
            return new LancamentoProducaoDTO(7L, 8L, quantidade, "obs");
        }

        private OrdemSetor prepararEtapa(EstatusOrdemProducao status) {
            OrdemSetor os = etapaAtiva(status);
            when(ordemSetorRepository.findById(ETAPA_ID)).thenReturn(Optional.of(os));
            return os;
        }

        private void prepararMaquinaEOperador() {
            when(maquinaRepository.findById(7L)).thenReturn(Optional.of(maquina7));
            when(operadorRepository.findById(8L)).thenReturn(Optional.of(operador8));
        }

        private void prepararProximoPasso() {
            when(roteiroPassoRepository.buscarProximoPasso(3L, 20))
                    .thenReturn(Optional.of(passo(30, setor6, null)));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("Quantidade zero ou negativa é rejeitada antes de qualquer consulta")
        void quantidadeInvalida(int quantidade) {
            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(quantidade)),
                    AppMessages.ERROR_QUANTITY_INVALID);

            verifyNoInteractions(ordemSetorRepository, lancamentoRepository);
        }

        @Test
        @DisplayName("DTO nulo ou quantidade nula é rejeitado")
        void dtoOuQuantidadeNulos() {
            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, null), AppMessages.ERROR_QUANTITY_INVALID);
            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID,
                    new LancamentoProducaoDTO(7L, 8L, null, null)), AppMessages.ERROR_QUANTITY_INVALID);

            verifyNoInteractions(ordemSetorRepository, lancamentoRepository);
        }

        @Test
        @DisplayName("Etapa que não está liberada/em andamento é rejeitada")
        void etapaIndisponivel() {
            prepararEtapa(EstatusOrdemProducao.DISPONIVEL);

            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1)), AppMessages.ERROR_ORDER_NOT_AVAILABLE);

            verify(lancamentoRepository, never()).save(any());
        }


        @Test
        @DisplayName("Linha de refugo (retrabalho) não aceita lançamento de produção")
        void linhaDeRefugoEhRejeitada() {
            OrdemSetor os = prepararEtapa(EstatusOrdemProducao.LIBERADA);
            os.setOrigemRefugo(true);

            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1)), AppMessages.ERROR_LAUNCH_REWORK_LINE);

            verify(lancamentoRepository, never()).save(any());
            verify(ordemSetorRepository, never()).save(any());
            verifyNoInteractions(maquinaRepository, operadorRepository);
        }

        @Test
        @DisplayName("Quantidade acima do pendente é rejeitada")
        void acimaDoPendente() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);

            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(11)),
                    AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);

            verify(lancamentoRepository, never()).save(any());
            verifyNoInteractions(maquinaRepository, operadorRepository);
        }

        @Test
        @DisplayName("Máquina inexistente ou de outro setor é rejeitada")
        void maquinaInvalida() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            when(maquinaRepository.findById(7L))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(maquina(7L, setor6)));

            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1)), AppMessages.ERROR_MACHINE_NOTFOUND);
            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1)), AppMessages.ERROR_MACHINE_NOTFOUND);

            verify(lancamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Operador inexistente ou com setor padrão diferente é rejeitado")
        void operadorInvalido() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            when(maquinaRepository.findById(7L)).thenReturn(Optional.of(maquina7));
            when(operadorRepository.findById(8L))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(operador(8L, setor6)));

            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1)), AppMessages.ERROR_OPERATOR_NOTFOUND);
            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1)), AppMessages.ERROR_OPERATOR_NOTFOUND);

            verify(lancamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Operador cujo setor padrão é o mesmo da etapa é aceito")
        void operadorDoMesmoSetor() {
            OrdemSetor os = prepararEtapa(EstatusOrdemProducao.LIBERADA);
            when(maquinaRepository.findById(7L)).thenReturn(Optional.of(maquina7));
            when(operadorRepository.findById(8L)).thenReturn(Optional.of(operador(8L, setor5)));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(2));

            assertThat(os.getQtdProduzida()).isEqualTo(2);
        }

        @Test
        @DisplayName("Ordem sem roteiro não permite lançar produção")
        void ordemSemRoteiro() {
            OrdemSetor os = prepararEtapa(EstatusOrdemProducao.LIBERADA);
            os.getOrdemProducao().setRoteiro(null);
            prepararMaquinaEOperador();

            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1)),
                    AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE);

            verify(lancamentoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Lançamento parcial: registra, passa a EM_ANDAMENTO, define início, atualiza saldos e alimenta a próxima etapa")
        void lancamentoParcial() {
            OrdemSetor os = prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            prepararProximoPasso();
            EtapaSetor etapaDoPasso = etapaSetor(1L, "SERRA", setor5);
            when(roteiroPassoRepository.findByRoteiroIdAndSequencia(3L, 20))
                    .thenReturn(Optional.of(passo(20, setor5, etapaDoPasso)));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);
            LocalDateTime antes = LocalDateTime.now();

            OrdemSetor retorno = service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(4));

            assertThat(retorno).isSameAs(os);
            assertThat(os.getStatus()).isEqualTo(EstatusOrdemProducao.EM_ANDAMENTO);
            assertThat(os.getQtdProduzida()).isEqualTo(4);
            assertThat(os.getQtdPendente()).isEqualTo(6);
            assertThat(os.getInicio()).isCloseTo(antes, within(5, java.time.temporal.ChronoUnit.SECONDS));
            assertThat(os.getFim()).isNull();

            ArgumentCaptor<Lancamento> lancamento = ArgumentCaptor.forClass(Lancamento.class);
            verify(lancamentoRepository).save(lancamento.capture());
            assertThat(lancamento.getValue().getQtdProduzida()).isEqualTo(4);
            assertThat(lancamento.getValue().getObservacao()).isEqualTo("obs");
            assertThat(lancamento.getValue().getOrdemSetor()).isSameAs(os);
            assertThat(lancamento.getValue().getMaquina()).isSameAs(maquina7);
            assertThat(lancamento.getValue().getOperador()).isSameAs(operador8);
            assertThat(lancamento.getValue().getEtapaSetor()).isSameAs(etapaDoPasso);

            ArgumentCaptor<OrdemSetor> salvas = ArgumentCaptor.forClass(OrdemSetor.class);
            verify(ordemSetorRepository, times(2)).save(salvas.capture());
            OrdemSetor proxima = salvas.getAllValues().get(1);
            assertThat(proxima.getSetor()).isSameAs(setor6);
            assertThat(proxima.getSequencia()).isEqualTo(30);
            assertThat(proxima.getVariante()).isEqualTo("A");
            assertThat(proxima.getOrdemProducao()).isSameAs(ordem10);
            assertThat(proxima.getQtdRecebida()).isEqualTo(4);
            assertThat(proxima.getQtdPendente()).isEqualTo(4);
            assertThat(proxima.getQtdProduzida()).isZero();
            assertThat(proxima.getStatus()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
            verify(ordemProducaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sem etapa cadastrada no passo, o lançamento fica sem etapaSetor")
        void semEtapaNoPasso() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1));

            ArgumentCaptor<Lancamento> lancamento = ArgumentCaptor.forClass(Lancamento.class);
            verify(lancamentoRepository).save(lancamento.capture());
            assertThat(lancamento.getValue().getEtapaSetor()).isNull();
        }

        @Test
        @DisplayName("Etapa já em andamento preserva o início original e permanece EM_ANDAMENTO")
        void preservaInicio() {
            OrdemSetor os = prepararEtapa(EstatusOrdemProducao.EM_ANDAMENTO);
            LocalDateTime inicio = LocalDateTime.of(2025, 1, 1, 8, 0);
            os.setInicio(inicio);
            prepararMaquinaEOperador();
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1));

            assertThat(os.getInicio()).isEqualTo(inicio);
            assertThat(os.getStatus()).isEqualTo(EstatusOrdemProducao.EM_ANDAMENTO);
        }

        @Test
        @DisplayName("Lançar exatamente o pendente conclui a etapa e define o fim")
        void lancamentoTotalConcluiEtapa() {
            OrdemSetor os = prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            prepararProximoPasso();
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);
            LocalDateTime antes = LocalDateTime.now();

            service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(10));

            assertThat(os.getStatus()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            assertThat(os.getQtdPendente()).isZero();
            assertThat(os.getQtdProduzida()).isEqualTo(10);
            assertThat(os.getFim()).isCloseTo(antes, within(5, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Última etapa do roteiro concluída sem etapas abertas conclui e salva a ordem")
        void ultimaEtapaConcluiAOrdem() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            when(roteiroPassoRepository.buscarProximoPasso(3L, 20)).thenReturn(Optional.empty());
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(false);

            service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(10));

            assertThat(ordem10.getStatus()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            verify(ordemProducaoRepository).save(ordem10);
            verify(ordemSetorRepository, times(1)).save(any(OrdemSetor.class));
        }

        @Test
        @DisplayName("Última etapa com outras linhas abertas (variantes) não conclui a ordem")
        void ultimaEtapaComOutrasLinhasAbertas() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            when(roteiroPassoRepository.buscarProximoPasso(3L, 20)).thenReturn(Optional.empty());
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(10));

            assertThat(ordem10.getStatus()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
            verify(ordemProducaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Próxima etapa já existente acumula a quantidade recebida e pendente sem recriar a linha")
        void acumulaNaProximaEtapaExistente() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            prepararProximoPasso();
            OrdemSetor proxima = etapa(300L, setor6, 30, 5, 2, EstatusOrdemProducao.EM_ANDAMENTO);
            when(ordemSetorRepository.findByOrdemProducaoIdAndSequenciaAndVariante(ORDEM_ID, 30, "A"))
                    .thenReturn(Optional.of(proxima));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(3));

            assertThat(proxima.getQtdRecebida()).isEqualTo(8);
            assertThat(proxima.getQtdPendente()).isEqualTo(6);
            assertThat(proxima.getQtdProduzida()).isEqualTo(2);
            assertThat(proxima.getStatus()).isEqualTo(EstatusOrdemProducao.EM_ANDAMENTO);
            verify(ordemSetorRepository).save(proxima);
        }

        @Test
        @DisplayName("Próxima etapa concluída é reaberta: EM_ANDAMENTO se já produziu, DISPONIVEL se não, e o fim é limpo")
        void reabreProximaEtapaConcluida() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            prepararProximoPasso();
            OrdemSetor jaProduziu = etapa(300L, setor6, 30, 5, 5, EstatusOrdemProducao.CONCLUIDA);
            jaProduziu.setFim(LocalDateTime.of(2025, 1, 1, 9, 0));
            when(ordemSetorRepository.findByOrdemProducaoIdAndSequenciaAndVariante(ORDEM_ID, 30, "A"))
                    .thenReturn(Optional.of(jaProduziu));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(3));

            assertThat(jaProduziu.getStatus()).isEqualTo(EstatusOrdemProducao.EM_ANDAMENTO);
            assertThat(jaProduziu.getFim()).isNull();
            assertThat(jaProduziu.getQtdRecebida()).isEqualTo(8);
            assertThat(jaProduziu.getQtdPendente()).isEqualTo(3);
        }

        @Test
        @DisplayName("Próxima etapa concluída sem produção própria volta para DISPONIVEL")
        void reabreProximaEtapaSemProducao() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            prepararProximoPasso();
            OrdemSetor semProducao = etapa(300L, setor6, 30, 0, 0, EstatusOrdemProducao.CONCLUIDA);
            semProducao.setFim(LocalDateTime.of(2025, 1, 1, 9, 0));
            when(ordemSetorRepository.findByOrdemProducaoIdAndSequenciaAndVariante(ORDEM_ID, 30, "A"))
                    .thenReturn(Optional.of(semProducao));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(3));

            assertThat(semProducao.getStatus()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
            assertThat(semProducao.getFim()).isNull();
        }

        @Test
        @DisplayName("Conflito de versão (lock otimista) é propagado sem conversão")
        void lockOtimistaEhPropagado() {
            OrdemSetor os = prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            ObjectOptimisticLockingFailureException conflito = new ObjectOptimisticLockingFailureException(OrdemSetor.class, 1L);
            when(ordemSetorRepository.save(os)).thenThrow(conflito);

            assertThatThrownBy(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1))).isSameAs(conflito);
        }

        @Test
        @DisplayName("Erro de banco vira pedido de nova tentativa")
        void erroDeBanco() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            prepararMaquinaEOperador();
            when(lancamentoRepository.save(any(Lancamento.class))).thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1)), AppMessages.ERROR_PERSISTENCE_RETRY);
        }

        @Test
        @DisplayName("Erro inesperado vira erro interno do sistema")
        void erroInesperado() {
            prepararEtapa(EstatusOrdemProducao.LIBERADA);
            when(maquinaRepository.findById(7L)).thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.lancarProducao(ORDEM_ID, ETAPA_ID, dto(1)), AppMessages.ERROR_SYSTEM_INTERNAL);
        }
    }

    // ---------- refugo ----------

    @Nested
    @DisplayName("registrarRefugo")
    class RegistrarRefugo {

        private final Operador operador8 = operador(8L, null);

        private RefugoDTO dto(int quantidade, DestinoRefugo destino, Long etapaSetorId) {
            return new RefugoDTO(quantidade, 5L, etapaSetorId, 8L, destino, "TRINCA");
        }

        /** Etapa atual: DOBRA (setor 6), sequência 30, 20 recebidas. */
        private OrdemSetor prepararEtapaAtual() {
            OrdemSetor os = etapa(ETAPA_ID, setor6, 30, 20, 0, EstatusOrdemProducao.LIBERADA);
            when(ordemSetorRepository.findById(ETAPA_ID)).thenReturn(Optional.of(os));
            return os;
        }

        private void prepararOrigemEOperador() {
            when(setorService.buscarSetorPorId(5L)).thenReturn(setor5);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);
        }

        private void prepararRoteiro(RoteiroPasso... passos) {
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(3L)).thenReturn(List.of(passos));
        }

        private void salvarRetornandoArgumento() {
            when(ordemSetorRepository.save(any(OrdemSetor.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -3})
        @DisplayName("Quantidade zero ou negativa é rejeitada")
        void quantidadeInvalida(int quantidade) {
            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(quantidade, DestinoRefugo.DESCARTE, null)),
                    AppMessages.ERROR_QUANTITY_INVALID);
            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, null), AppMessages.ERROR_QUANTITY_INVALID);

            verifyNoInteractions(ordemSetorRepository, refugoRepository);
        }

        @Test
        @DisplayName("Operador com setor padrão diferente do setor da etapa é rejeitado")
        void operadorDeOutroSetor() {
            prepararEtapaAtual();
            when(setorService.buscarSetorPorId(5L)).thenReturn(setor5);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador(8L, setor5));

            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(1, DestinoRefugo.DESCARTE, null)),
                    AppMessages.ERROR_OPERATOR_NOTFOUND);

            verify(refugoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Quantidade acima do pendente é rejeitada")
        void acimaDoPendente() {
            prepararEtapaAtual();
            prepararOrigemEOperador();

            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(21, DestinoRefugo.DESCARTE, null)),
                    AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);

            verify(refugoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Setor de origem fora do roteiro é rejeitado")
        void origemForaDoRoteiro() {
            prepararEtapaAtual();
            prepararOrigemEOperador();
            prepararRoteiro(passo(30, setor6, null));

            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(1, DestinoRefugo.DESCARTE, null)),
                    AppMessages.ERROR_ROUTE_STEP_NOTFOUND);

            verify(refugoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Setor com mais de um passo exige a etapa de origem")
        void exigeEtapaQuandoSetorRepetido() {
            prepararEtapaAtual();
            prepararOrigemEOperador();
            prepararRoteiro(
                    passo(10, setor5, etapaSetor(1L, "1ª", setor5)),
                    passo(20, setor5, etapaSetor(2L, "2ª", setor5)),
                    passo(30, setor6, null));

            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(1, DestinoRefugo.DESCARTE, null)),
                    AppMessages.ERROR_REFUGO_ETAPA_REQUIRED);
        }

        @Test
        @DisplayName("Etapa informada que não pertence aos passos do setor é rejeitada")
        void etapaInexistenteNoSetor() {
            prepararEtapaAtual();
            prepararOrigemEOperador();
            prepararRoteiro(
                    passo(10, setor5, etapaSetor(1L, "1ª", setor5)),
                    passo(20, setor5, etapaSetor(2L, "2ª", setor5)),
                    passo(30, setor6, null));

            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(1, DestinoRefugo.DESCARTE, 99L)),
                    AppMessages.ERROR_ROUTE_STEP_NOTFOUND);
        }

        @Test
        @DisplayName("Origem em passo posterior ao da etapa atual é rejeitada")
        void origemPosterior() {
            OrdemSetor os = etapa(ETAPA_ID, setor6, 10, 20, 0, EstatusOrdemProducao.LIBERADA);
            when(ordemSetorRepository.findById(ETAPA_ID)).thenReturn(Optional.of(os));
            prepararOrigemEOperador();
            prepararRoteiro(passo(10, setor6, null), passo(20, setor5, null));

            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(1, DestinoRefugo.DESCARTE, null)),
                    AppMessages.ERROR_REFUGO_ORIGEM_INVALIDA);

            verify(refugoRepository, never()).save(any());
            assertThat(os.getQtdRecebida()).as("nada deve ser alterado").isEqualTo(20);
        }

        @Test
        @DisplayName("Retrabalho: baixa a etapa atual, cria linha de refugo no setor de origem e grava o refugo")
        void refugoComRetrabalho() {
            OrdemSetor atual = prepararEtapaAtual();
            prepararOrigemEOperador();
            prepararRoteiro(passo(20, setor5, null), passo(30, setor6, null));
            salvarRetornandoArgumento();
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);
            LocalDateTime antes = LocalDateTime.now();

            OrdemSetor retorno = service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(5, DestinoRefugo.RETRABALHO, null));

            assertThat(retorno).isSameAs(atual);
            assertThat(atual.getQtdRecebida()).isEqualTo(15);
            assertThat(atual.getQtdPendente()).isEqualTo(15);
            assertThat(atual.getStatus()).isEqualTo(EstatusOrdemProducao.LIBERADA);

            ArgumentCaptor<OrdemSetor> salvas = ArgumentCaptor.forClass(OrdemSetor.class);
            verify(ordemSetorRepository, times(2)).save(salvas.capture());
            OrdemSetor linhaRefugo = salvas.getAllValues().get(1);
            assertThat(linhaRefugo.getSetor()).isSameAs(setor5);
            assertThat(linhaRefugo.getSequencia()).isEqualTo(20);
            assertThat(linhaRefugo.getVariante()).isEqualTo("A");
            assertThat(linhaRefugo.getQtdRecebida()).isEqualTo(5);
            assertThat(linhaRefugo.getQtdPendente()).isEqualTo(5);
            assertThat(linhaRefugo.getQtdProduzida()).isZero();
            assertThat(linhaRefugo.getStatus()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
            assertThat(linhaRefugo.isOrigemRefugo()).isTrue();
            assertThat(linhaRefugo.getOrdemProducao()).isSameAs(ordem10);

            ArgumentCaptor<Refugo> refugo = ArgumentCaptor.forClass(Refugo.class);
            verify(refugoRepository).save(refugo.capture());
            Refugo gravado = refugo.getValue();
            assertThat(gravado.getQtdRefugo()).isEqualTo(5);
            assertThat(gravado.getMotivo()).isEqualTo("TRINCA");
            assertThat(gravado.getDestino()).isEqualTo(DestinoRefugo.RETRABALHO);
            assertThat(gravado.getVariante()).isEqualTo("A");
            assertThat(gravado.getOrdemSetor()).isSameAs(atual);
            assertThat(gravado.getSetorOrigem()).isSameAs(setor5);
            assertThat(gravado.getSequenciaOrigem()).isEqualTo(20);
            assertThat(gravado.getOperador()).isSameAs(operador8);
            assertThat(gravado.getOrdemSetorRetrabalho()).isSameAs(linhaRefugo);
            assertThat(gravado.getDataHora()).isCloseTo(antes, within(5, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("Descarte: baixa a etapa atual sem criar linha de retrabalho")
        void refugoComDescarte() {
            OrdemSetor atual = prepararEtapaAtual();
            prepararOrigemEOperador();
            prepararRoteiro(passo(20, setor5, null), passo(30, setor6, null));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(5, DestinoRefugo.DESCARTE, null));

            assertThat(atual.getQtdRecebida()).isEqualTo(15);
            assertThat(atual.getQtdPendente()).isEqualTo(15);
            verify(ordemSetorRepository, times(1)).save(atual);
            ArgumentCaptor<Refugo> refugo = ArgumentCaptor.forClass(Refugo.class);
            verify(refugoRepository).save(refugo.capture());
            assertThat(refugo.getValue().getOrdemSetorRetrabalho()).isNull();
            assertThat(refugo.getValue().getDestino()).isEqualTo(DestinoRefugo.DESCARTE);
        }

        @Test
        @DisplayName("Descartar todo o pendente conclui a etapa e, sem etapas abertas, a ordem")
        void descarteTotalConcluiEtapaEOrdem() {
            OrdemSetor atual = prepararEtapaAtual();
            prepararOrigemEOperador();
            prepararRoteiro(passo(20, setor5, null), passo(30, setor6, null));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(false);

            service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(20, DestinoRefugo.DESCARTE, null));

            assertThat(atual.getStatus()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            assertThat(atual.getFim()).isNotNull();
            assertThat(atual.getQtdPendente()).isZero();
            assertThat(ordem10.getStatus()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            verify(ordemProducaoRepository).save(ordem10);
        }

        @Test
        @DisplayName("Retrabalho de todo o pendente conclui a etapa atual mas mantém a ordem aberta (nova linha pendente)")
        void retrabalhoTotalMantemOrdemAberta() {
            OrdemSetor atual = prepararEtapaAtual();
            prepararOrigemEOperador();
            prepararRoteiro(passo(20, setor5, null), passo(30, setor6, null));
            salvarRetornandoArgumento();
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(20, DestinoRefugo.RETRABALHO, null));

            assertThat(atual.getStatus()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            assertThat(ordem10.getStatus()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
            verify(ordemProducaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Refugo originado na própria etapa (mesma sequência) é permitido")
        void origemNaMesmaSequencia() {
            prepararEtapaAtual();
            when(setorService.buscarSetorPorId(5L)).thenReturn(setor6);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);
            prepararRoteiro(passo(30, setor6, null));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(1, DestinoRefugo.DESCARTE, null));

            ArgumentCaptor<Refugo> refugo = ArgumentCaptor.forClass(Refugo.class);
            verify(refugoRepository).save(refugo.capture());
            assertThat(refugo.getValue().getSequenciaOrigem()).isEqualTo(30);
        }

        @Test
        @DisplayName("Setor com passos repetidos usa a etapa informada para escolher a sequência de origem")
        void escolhePassoPelaEtapa() {
            prepararEtapaAtual();
            prepararOrigemEOperador();
            prepararRoteiro(
                    passo(10, setor5, etapaSetor(1L, "1ª", setor5)),
                    passo(20, setor5, etapaSetor(2L, "2ª", setor5)),
                    passo(30, setor6, null));
            salvarRetornandoArgumento();
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(2, DestinoRefugo.RETRABALHO, 1L));

            ArgumentCaptor<Refugo> refugo = ArgumentCaptor.forClass(Refugo.class);
            verify(refugoRepository).save(refugo.capture());
            assertThat(refugo.getValue().getSequenciaOrigem()).isEqualTo(10);
        }


        @Test
        @DisplayName("Conflito de versão (lock otimista) é propagado sem conversão")
        void lockOtimistaEhPropagado() {
            OrdemSetor atual = prepararEtapaAtual();
            prepararOrigemEOperador();
            prepararRoteiro(passo(20, setor5, null), passo(30, setor6, null));
            ObjectOptimisticLockingFailureException conflito = new ObjectOptimisticLockingFailureException(OrdemSetor.class, 1L);
            when(ordemSetorRepository.save(atual)).thenThrow(conflito);

            assertThatThrownBy(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(1, DestinoRefugo.DESCARTE, null)))
                    .isSameAs(conflito);

            verify(refugoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Erro de banco vira pedido de nova tentativa e erro inesperado vira erro interno")
        void erros() {
            prepararEtapaAtual();
            when(setorService.buscarSetorPorId(5L))
                    .thenThrow(new DataRetrievalFailureException("x"))
                    .thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(1, DestinoRefugo.DESCARTE, null)),
                    AppMessages.ERROR_PERSISTENCE_RETRY);
            assertChave(() -> service.registrarRefugo(ORDEM_ID, ETAPA_ID, dto(1, DestinoRefugo.DESCARTE, null)),
                    AppMessages.ERROR_SYSTEM_INTERNAL);
        }
    }

    // ---------- retrabalho ----------

    @Nested
    @DisplayName("registrarRetrabalho")
    class RegistrarRetrabalho {

        private final Operador operador8 = operador(8L, null);
        private final Maquina maquina7 = maquina(7L, setor5);
        private OrdemSetor linhaRetrabalho;
        private Refugo refugo;

        private RetrabalhoDTO dto(int quantidade, Long maquinaId) {
            return new RetrabalhoDTO(50L, maquinaId, 8L, quantidade, "refeito");
        }

        /** Refugo de 5 peças com destino RETRABALHO originado no setor CORTE (seq. 20). */
        private void prepararRefugo() {
            linhaRetrabalho = etapa(200L, setor5, 20, 5, 0, EstatusOrdemProducao.LIBERADA);
            linhaRetrabalho.setOrigemRefugo(true);
            OrdemSetor onde = etapa(ETAPA_ID, setor6, 30, 20, 0, EstatusOrdemProducao.LIBERADA);
            refugo = new Refugo();
            refugo.setId(50L);
            refugo.setQtdRefugo(5);
            refugo.setDestino(DestinoRefugo.RETRABALHO);
            refugo.setSetorOrigem(setor5);
            refugo.setSequenciaOrigem(20);
            refugo.setOrdemSetor(onde);
            refugo.setOrdemSetorRetrabalho(linhaRetrabalho);
            when(refugoRepository.findById(50L)).thenReturn(Optional.of(refugo));
        }

        private void prepararSomaJaRetrabalhada(int jaRetrabalhado) {
            when(retrabalhoRepository.somarRetrabalhadoPorRefugo(50L)).thenReturn(jaRetrabalhado);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        @DisplayName("Quantidade zero ou negativa é rejeitada")
        void quantidadeInvalida(int quantidade) {
            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(quantidade, 7L)), AppMessages.ERROR_QUANTITY_INVALID);
            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, null), AppMessages.ERROR_QUANTITY_INVALID);

            verifyNoInteractions(refugoRepository);
        }

        @Test
        @DisplayName("Refugo inexistente é rejeitado")
        void refugoInexistente() {
            when(refugoRepository.findById(50L)).thenReturn(Optional.empty());

            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(1, 7L)), AppMessages.ERROR_REFUGO_NOTFOUND);
        }

        @Test
        @DisplayName("Refugo de outra ordem é rejeitado")
        void refugoDeOutraOrdem() {
            prepararRefugo();

            assertChave(() -> service.registrarRetrabalho(999L, dto(1, 7L)), AppMessages.ERROR_REFUGO_NOTFOUND);
        }

        @Test
        @DisplayName("Refugo com destino DESCARTE não pode ser retrabalhado")
        void refugoDescartado() {
            prepararRefugo();
            refugo.setDestino(DestinoRefugo.DESCARTE);

            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(1, 7L)), AppMessages.ERROR_REFUGO_NOTFOUND);
        }

        @Test
        @DisplayName("Quantidade acima do saldo do refugo (refugado - já retrabalhado) é rejeitada")
        void acimaDoSaldoDoRefugo() {
            prepararRefugo();
            prepararSomaJaRetrabalhada(4);

            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(2, 7L)), AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);

            verify(retrabalhoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Máquina de outro setor ou inexistente é rejeitada")
        void maquinaInvalida() {
            prepararRefugo();
            prepararSomaJaRetrabalhada(0);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);
            when(maquinaRepository.findById(7L))
                    .thenReturn(Optional.of(maquina(7L, setor6)))
                    .thenReturn(Optional.empty());

            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(1, 7L)), AppMessages.ERROR_MACHINE_NOTFOUND);
            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(1, 7L)), AppMessages.ERROR_MACHINE_NOTFOUND);

            verify(retrabalhoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Refugo sem linha de retrabalho vinculada é rejeitado")
        void semLinhaDeRetrabalho() {
            prepararRefugo();
            refugo.setOrdemSetorRetrabalho(null);
            prepararSomaJaRetrabalhada(0);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);

            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(1, null)), AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("Quantidade acima do pendente da linha de retrabalho é rejeitada")
        void acimaDoPendenteDaLinha() {
            prepararRefugo();
            linhaRetrabalho.setQtdPendente(1);
            prepararSomaJaRetrabalhada(0);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);

            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(2, null)), AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);

            verify(retrabalhoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Retrabalho parcial: grava, atualiza saldos, mantém EM_ANDAMENTO e envia peças à etapa seguinte")
        void retrabalhoParcial() {
            prepararRefugo();
            prepararSomaJaRetrabalhada(1);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);
            when(maquinaRepository.findById(7L)).thenReturn(Optional.of(maquina7));
            when(roteiroPassoRepository.buscarProximoPasso(3L, 20)).thenReturn(Optional.of(passo(30, setor6, null)));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);
            LocalDateTime antes = LocalDateTime.now();

            OrdemSetor retorno = service.registrarRetrabalho(ORDEM_ID, dto(3, 7L));

            assertThat(retorno).isSameAs(linhaRetrabalho);
            assertThat(linhaRetrabalho.getQtdProduzida()).isEqualTo(3);
            assertThat(linhaRetrabalho.getQtdPendente()).isEqualTo(2);
            assertThat(linhaRetrabalho.getStatus()).isEqualTo(EstatusOrdemProducao.EM_ANDAMENTO);
            assertThat(linhaRetrabalho.getFim()).isNull();

            ArgumentCaptor<Retrabalho> retrabalho = ArgumentCaptor.forClass(Retrabalho.class);
            verify(retrabalhoRepository).save(retrabalho.capture());
            assertThat(retrabalho.getValue().getQtdRefeita()).isEqualTo(3);
            assertThat(retrabalho.getValue().getObservacao()).isEqualTo("refeito");
            assertThat(retrabalho.getValue().getRefugo()).isSameAs(refugo);
            assertThat(retrabalho.getValue().getMaquina()).isSameAs(maquina7);
            assertThat(retrabalho.getValue().getOperador()).isSameAs(operador8);
            assertThat(retrabalho.getValue().getDataHora())
                    .isCloseTo(antes, within(5, java.time.temporal.ChronoUnit.SECONDS));

            ArgumentCaptor<OrdemSetor> salvas = ArgumentCaptor.forClass(OrdemSetor.class);
            verify(ordemSetorRepository, times(2)).save(salvas.capture());
            assertThat(salvas.getAllValues().get(0)).isSameAs(linhaRetrabalho);
            OrdemSetor proxima = salvas.getAllValues().get(1);
            assertThat(proxima.getSequencia()).isEqualTo(30);
            assertThat(proxima.getQtdRecebida()).isEqualTo(3);
            assertThat(proxima.getQtdPendente()).isEqualTo(3);
            assertThat(proxima.getVariante()).isEqualTo("A");
        }

        @Test
        @DisplayName("Retrabalho total conclui a linha de retrabalho e define o fim; máquina é opcional")
        void retrabalhoTotalSemMaquina() {
            prepararRefugo();
            prepararSomaJaRetrabalhada(0);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);
            when(roteiroPassoRepository.buscarProximoPasso(3L, 20)).thenReturn(Optional.of(passo(30, setor6, null)));
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);

            service.registrarRetrabalho(ORDEM_ID, dto(5, null));

            assertThat(linhaRetrabalho.getStatus()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            assertThat(linhaRetrabalho.getFim()).isNotNull();
            assertThat(linhaRetrabalho.getQtdPendente()).isZero();
            ArgumentCaptor<Retrabalho> retrabalho = ArgumentCaptor.forClass(Retrabalho.class);
            verify(retrabalhoRepository).save(retrabalho.capture());
            assertThat(retrabalho.getValue().getMaquina()).isNull();
            verifyNoInteractions(maquinaRepository);
        }

        @Test
        @DisplayName("Última etapa do roteiro concluída no retrabalho e sem linhas abertas conclui a ordem")
        void retrabalhoConcluiOrdem() {
            prepararRefugo();
            prepararSomaJaRetrabalhada(0);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);
            when(roteiroPassoRepository.buscarProximoPasso(3L, 20)).thenReturn(Optional.empty());
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(false);

            service.registrarRetrabalho(ORDEM_ID, dto(5, null));

            assertThat(ordem10.getStatus()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            verify(ordemProducaoRepository).save(ordem10);
        }


        @ParameterizedTest
        @EnumSource(value = EstatusOrdemProducao.class, names = {"DISPONIVEL", "CONCLUIDA"})
        @DisplayName("Linha de retrabalho que não está liberada/em andamento é rejeitada")
        void linhaForaDeEtapaAtiva(EstatusOrdemProducao status) {
            prepararRefugo();
            linhaRetrabalho.setStatus(status);
            prepararSomaJaRetrabalhada(0);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);

            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(1, null)), AppMessages.ERROR_ORDER_NOT_AVAILABLE);

            verify(retrabalhoRepository, never()).save(any());
            verify(ordemSetorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Primeiro retrabalho define o início da linha; os seguintes preservam o início original")
        void preencheInicioSeNulo() {
            prepararRefugo();
            prepararSomaJaRetrabalhada(0);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);
            when(roteiroPassoRepository.buscarProximoPasso(3L, 20)).thenReturn(Optional.empty());
            when(ordemSetorRepository.existsByOrdemProducaoIdAndStatusNot(ORDEM_ID, EstatusOrdemProducao.CONCLUIDA))
                    .thenReturn(true);
            LocalDateTime antes = LocalDateTime.now();

            service.registrarRetrabalho(ORDEM_ID, dto(1, null));

            assertThat(linhaRetrabalho.getInicio())
                    .isCloseTo(antes, within(5, java.time.temporal.ChronoUnit.SECONDS));

            LocalDateTime inicioOriginal = LocalDateTime.of(2024, 1, 2, 8, 0);
            linhaRetrabalho.setInicio(inicioOriginal);
            prepararSomaJaRetrabalhada(1);

            service.registrarRetrabalho(ORDEM_ID, dto(1, null));

            assertThat(linhaRetrabalho.getInicio()).isEqualTo(inicioOriginal);
        }

        @Test
        @DisplayName("Conflito de versão (lock otimista) é propagado sem conversão")
        void lockOtimistaEhPropagado() {
            prepararRefugo();
            prepararSomaJaRetrabalhada(0);
            when(operadorService.buscarOperadorPorId(8L)).thenReturn(operador8);
            ObjectOptimisticLockingFailureException conflito = new ObjectOptimisticLockingFailureException(OrdemSetor.class, 200L);
            when(ordemSetorRepository.save(linhaRetrabalho)).thenThrow(conflito);

            assertThatThrownBy(() -> service.registrarRetrabalho(ORDEM_ID, dto(1, null))).isSameAs(conflito);
        }

        @Test
        @DisplayName("Erro de banco vira pedido de nova tentativa e erro inesperado vira erro interno")
        void erros() {
            when(refugoRepository.findById(50L))
                    .thenThrow(new DataRetrievalFailureException("x"))
                    .thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(1, null)), AppMessages.ERROR_PERSISTENCE_RETRY);
            assertChave(() -> service.registrarRetrabalho(ORDEM_ID, dto(1, null)), AppMessages.ERROR_SYSTEM_INTERNAL);
        }
    }
}
