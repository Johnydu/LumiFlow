package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.dashboard.DashboardOrdemDTO;
import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.dto.roteiro.PassoDoRoteiroDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.Produto;
import br.com.lumiflow.entity.Roteiro;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import br.com.lumiflow.repository.RefugoRepository;
import br.com.lumiflow.repository.RetrabalhoRepository;
import br.com.lumiflow.repository.RoteiroPassoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private OrdemProducaoRepository ordemProducaoRepository;
    @Mock
    private RoteiroPassoRepository roteiroPassoRepository;
    @Mock
    private OrdemSetorRepository ordemSetorRepository;
    @Mock
    private RefugoRepository refugoRepository;
    @Mock
    private RetrabalhoRepository retrabalhoRepository;
    @Spy
    private Clock clock = RelogioTeste.fixoEm(LocalDateTime.of(2025, 6, 15, 23, 59, 59));

    @InjectMocks
    private DashboardService service;

    private static void assertChave(Runnable acao, String chave) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", chave);
    }

    @Nested
    @DisplayName("obterResumoEstatisticas")
    class Resumo {

        private void stubRetrabalho(long refugos, long refeitas) {
            when(refugoRepository.somarQuantidadePorDestino(DestinoRefugo.RETRABALHO)).thenReturn(refugos);
            when(retrabalhoRepository.somarQuantidadePorDestino(DestinoRefugo.RETRABALHO)).thenReturn(refeitas);
        }

        @Test
        @DisplayName("Reúne contagens e calcula peças em retrabalho (refugos - refeitas)")
        void resumoCompleto() {
            when(ordemSetorRepository.countByStatusNot(EstatusOrdemProducao.CONCLUIDA)).thenReturn(12L);
            when(ordemSetorRepository.countByStatus(EstatusOrdemProducao.EM_ANDAMENTO)).thenReturn(4L);
            when(ordemSetorRepository.countByStatusAndFimGreaterThanEqualAndFimLessThan(eq(EstatusOrdemProducao.CONCLUIDA), any(), any()))
                    .thenReturn(3L);
            stubRetrabalho(10, 4);

            DashboardResumoDTO resumo = service.obterResumoEstatisticas();

            assertThat(resumo).isEqualTo(new DashboardResumoDTO(12L, 4L, 3L, 6L));
        }

        @Test
        @DisplayName("Concluídas hoje usa o intervalo semiaberto [início do dia, início do dia seguinte) pelo Clock")
        void intervaloDeHoje() {
            when(ordemSetorRepository.countByStatusAndFimGreaterThanEqualAndFimLessThan(eq(EstatusOrdemProducao.CONCLUIDA), any(), any()))
                    .thenReturn(0L);
            stubRetrabalho(0, 0);

            service.obterResumoEstatisticas();

            ArgumentCaptor<LocalDateTime> inicio = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> fim = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(ordemSetorRepository).countByStatusAndFimGreaterThanEqualAndFimLessThan(eq(EstatusOrdemProducao.CONCLUIDA),
                    inicio.capture(), fim.capture());
            // O relógio de teste está em 15/06 às 23:59:59: o último segundo do dia ainda é "hoje".
            assertThat(inicio.getValue()).isEqualTo(LocalDateTime.of(2025, 6, 15, 0, 0));
            assertThat(fim.getValue()).isEqualTo(LocalDateTime.of(2025, 6, 16, 0, 0));
        }

        @Test
        @DisplayName("Contagens nulas viram zero")
        void contagensNulas() {
            when(ordemSetorRepository.countByStatusNot(EstatusOrdemProducao.CONCLUIDA)).thenReturn(null);
            when(ordemSetorRepository.countByStatus(EstatusOrdemProducao.EM_ANDAMENTO)).thenReturn(null);
            when(ordemSetorRepository.countByStatusAndFimGreaterThanEqualAndFimLessThan(eq(EstatusOrdemProducao.CONCLUIDA), any(), any()))
                    .thenReturn(null);
            stubRetrabalho(0, 0);

            assertThat(service.obterResumoEstatisticas()).isEqualTo(new DashboardResumoDTO(0L, 0L, 0L, 0L));
        }

        @Test
        @DisplayName("Mais peças refeitas do que refugadas nunca gera retrabalho negativo")
        void retrabalhoNaoFicaNegativo() {
            stubRetrabalho(3, 8);

            assertThat(service.contarPecasEmRetrabalhoAberto()).isZero();
        }

        @Test
        @DisplayName("Erros de banco viram falha de validação")
        void erros() {
            when(ordemSetorRepository.countByStatusNot(EstatusOrdemProducao.CONCLUIDA))
                    .thenThrow(new DataRetrievalFailureException("x"));
            when(refugoRepository.somarQuantidadePorDestino(DestinoRefugo.RETRABALHO))
                    .thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.obterResumoEstatisticas(), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.contarPecasEmRetrabalhoAberto(), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("obterResumoSetores")
    class ResumoSetores {

        @Test
        @DisplayName("Retorna o resumo do repositório")
        void retornaResumo() {
            List<SetorResumoDTO> resumo = List.of(new SetorResumoDTO(1L, "CORTE", 3));
            when(ordemSetorRepository.contarEtapasDisponiveisPorSetor()).thenReturn(resumo);

            assertThat(service.obterResumoSetores()).isSameAs(resumo);
        }

        @Test
        @DisplayName("Resultado nulo vira lista vazia e erro vira falha de validação")
        void nuloEErro() {
            when(ordemSetorRepository.contarEtapasDisponiveisPorSetor())
                    .thenReturn(null)
                    .thenThrow(new DataRetrievalFailureException("x"));

            assertThat(service.obterResumoSetores()).isEmpty();
            assertChave(() -> service.obterResumoSetores(), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("listarOrdens")
    class ListarOrdens {

        private final PageRequest pagina0 = PageRequest.of(0, 10);

        private Roteiro roteiro(Long id) {
            Roteiro r = new Roteiro();
            r.setId(id);
            return r;
        }

        private OrdemProducao ordem(Long id, Roteiro roteiro, EstatusOrdemProducao status) {
            Produto produto = new Produto();
            produto.setNome("PORTA");
            produto.setCodigo("P-01");
            OrdemProducao o = new OrdemProducao();
            o.setId(id);
            o.setNumero("OP-000" + id);
            o.setQuantidade(50);
            o.setRoteiro(roteiro);
            o.setProduto(produto);
            o.setStatus(status);
            return o;
        }

        private OrdemSetor linha(OrdemProducao ordem, int sequencia, EstatusOrdemProducao status, String setorNome) {
            OrdemSetor os = new OrdemSetor();
            os.setOrdemProducao(ordem);
            os.setSequencia(sequencia);
            os.setStatus(status);
            if (setorNome != null) {
                Setor s = new Setor();
                s.setNome(setorNome);
                os.setSetor(s);
            }
            return os;
        }

        private void stubPagina(OrdemProducao... ordens) {
            when(ordemProducaoRepository.buscarParaDashboard(eq(""), any(), eq(pagina0)))
                    .thenReturn(new PageImpl<>(List.of(ordens), pagina0, ordens.length));
        }

        @Test
        @DisplayName("Filtro ABERTAS, CONCLUIDAS e qualquer outro valor selecionam os status corretos")
        void filtrosDeStatus() {
            when(ordemProducaoRepository.buscarParaDashboard(eq("x"), any(), eq(pagina0))).thenReturn(Page.empty(pagina0));

            service.listarOrdens(" x ", "abertas", 0);
            service.listarOrdens("x", "CONCLUIDAS", 0);
            service.listarOrdens("x", null, 0);
            service.listarOrdens("x", "QUALQUER", 0);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<EstatusOrdemProducao>> statuses = ArgumentCaptor.forClass(Collection.class);
            verify(ordemProducaoRepository, org.mockito.Mockito.times(4))
                    .buscarParaDashboard(eq("x"), statuses.capture(), eq(pagina0));
            List<Collection<EstatusOrdemProducao>> valores = statuses.getAllValues();
            assertThat(valores.get(0)).containsExactlyInAnyOrder(EstatusOrdemProducao.DISPONIVEL,
                    EstatusOrdemProducao.LIBERADA, EstatusOrdemProducao.EM_ANDAMENTO);
            assertThat(valores.get(1)).containsExactly(EstatusOrdemProducao.CONCLUIDA);
            assertThat(valores.get(2)).containsExactlyInAnyOrder(EstatusOrdemProducao.values());
            assertThat(valores.get(3)).containsExactlyInAnyOrder(EstatusOrdemProducao.values());
        }

        @Test
        @DisplayName("Página vazia retorna vazio sem consultar etapas nem roteiros")
        void paginaVazia() {
            when(ordemProducaoRepository.buscarParaDashboard(eq(""), any(), eq(pagina0)))
                    .thenReturn(new PageImpl<>(List.of(), pagina0, 0));

            Page<DashboardOrdemDTO> resultado = service.listarOrdens(null, null, -1);

            assertThat(resultado).isEmpty();
            verifyNoInteractions(ordemSetorRepository, roteiroPassoRepository);
        }

        @Test
        @DisplayName("Ordem em andamento: progresso por etapas concluídas, etapa atual e setor atual")
        void ordemEmAndamento() {
            Roteiro roteiro = roteiro(3L);
            OrdemProducao ordem = ordem(1L, roteiro, EstatusOrdemProducao.DISPONIVEL);
            stubPagina(ordem);
            when(ordemSetorRepository.findByOrdemProducaoIdInOrderBySequenciaAsc(List.of(1L))).thenReturn(List.of(
                    linha(ordem, 10, EstatusOrdemProducao.CONCLUIDA, "CORTE"),
                    linha(ordem, 20, EstatusOrdemProducao.EM_ANDAMENTO, "DOBRA")));
            when(roteiroPassoRepository.buscarPassosPorRoteiroIds(Set.of(3L))).thenReturn(List.of(
                    new PassoDoRoteiroDTO(3L, 10, null),
                    new PassoDoRoteiroDTO(3L, 20, "1ª DOBRA"),
                    new PassoDoRoteiroDTO(3L, 30, null)));

            DashboardOrdemDTO dto = service.listarOrdens(null, null, 0).getContent().get(0);

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.numero()).isEqualTo("OP-0001");
            assertThat(dto.produtoNome()).isEqualTo("PORTA");
            assertThat(dto.produtoCodigo()).isEqualTo("P-01");
            assertThat(dto.quantidade()).isEqualTo(50);
            assertThat(dto.status()).isEqualTo(EstatusOrdemProducao.EM_ANDAMENTO);
            assertThat(dto.totalEtapas()).isEqualTo(3);
            assertThat(dto.etapasConcluidas()).isEqualTo(1);
            assertThat(dto.progresso()).isCloseTo(100.0 / 3, within(0.0001));
            assertThat(dto.etapaAtualNome()).isEqualTo("1ª DOBRA");
            assertThat(dto.setorAtualNome()).isEqualTo("DOBRA");
        }

        @Test
        @DisplayName("Ordem concluída: todas as etapas, 100% e sem etapa atual")
        void ordemConcluida() {
            Roteiro roteiro = roteiro(3L);
            OrdemProducao ordem = ordem(1L, roteiro, EstatusOrdemProducao.CONCLUIDA);
            stubPagina(ordem);
            when(ordemSetorRepository.findByOrdemProducaoIdInOrderBySequenciaAsc(List.of(1L))).thenReturn(List.of(
                    linha(ordem, 10, EstatusOrdemProducao.CONCLUIDA, "CORTE")));
            when(roteiroPassoRepository.buscarPassosPorRoteiroIds(Set.of(3L))).thenReturn(List.of(
                    new PassoDoRoteiroDTO(3L, 10, null), new PassoDoRoteiroDTO(3L, 20, null)));

            DashboardOrdemDTO dto = service.listarOrdens(null, null, 0).getContent().get(0);

            assertThat(dto.status()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            assertThat(dto.etapasConcluidas()).isEqualTo(2);
            assertThat(dto.totalEtapas()).isEqualTo(2);
            assertThat(dto.progresso()).isEqualTo(100.0);
            assertThat(dto.etapaAtualNome()).isNull();
            assertThat(dto.setorAtualNome()).isNull();
        }

        @Test
        @DisplayName("Variantes na mesma sequência: etapa só conta como concluída se todas as linhas estiverem concluídas")
        void variantesNaMesmaSequencia() {
            Roteiro roteiro = roteiro(3L);
            OrdemProducao ordem = ordem(1L, roteiro, EstatusOrdemProducao.DISPONIVEL);
            stubPagina(ordem);
            when(ordemSetorRepository.findByOrdemProducaoIdInOrderBySequenciaAsc(List.of(1L))).thenReturn(List.of(
                    linha(ordem, 10, EstatusOrdemProducao.CONCLUIDA, "CORTE"),
                    linha(ordem, 10, EstatusOrdemProducao.DISPONIVEL, "CORTE")));
            when(roteiroPassoRepository.buscarPassosPorRoteiroIds(Set.of(3L))).thenReturn(List.of(
                    new PassoDoRoteiroDTO(3L, 10, null), new PassoDoRoteiroDTO(3L, 20, null)));

            DashboardOrdemDTO dto = service.listarOrdens(null, null, 0).getContent().get(0);

            assertThat(dto.etapasConcluidas()).isZero();
            assertThat(dto.progresso()).isZero();
            assertThat(dto.status()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
        }

        @Test
        @DisplayName("Ordem sem etapas ainda tem progresso zero e status DISPONIVEL")
        void ordemSemEtapas() {
            Roteiro roteiro = roteiro(3L);
            OrdemProducao ordem = ordem(1L, roteiro, EstatusOrdemProducao.DISPONIVEL);
            stubPagina(ordem);
            when(ordemSetorRepository.findByOrdemProducaoIdInOrderBySequenciaAsc(List.of(1L))).thenReturn(List.of());
            when(roteiroPassoRepository.buscarPassosPorRoteiroIds(Set.of(3L)))
                    .thenReturn(List.of(new PassoDoRoteiroDTO(3L, 10, null)));

            DashboardOrdemDTO dto = service.listarOrdens(null, null, 0).getContent().get(0);

            assertThat(dto.etapasConcluidas()).isZero();
            assertThat(dto.totalEtapas()).isEqualTo(1);
            assertThat(dto.progresso()).isZero();
            assertThat(dto.etapaAtualNome()).isNull();
            assertThat(dto.status()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
        }

        @Test
        @DisplayName("Ordem sem roteiro e sem produto não consulta passos e retorna campos nulos com progresso zero")
        void ordemSemRoteiroESemProduto() {
            OrdemProducao ordem = ordem(1L, null, EstatusOrdemProducao.DISPONIVEL);
            ordem.setProduto(null);
            stubPagina(ordem);
            when(ordemSetorRepository.findByOrdemProducaoIdInOrderBySequenciaAsc(List.of(1L))).thenReturn(List.of());

            DashboardOrdemDTO dto = service.listarOrdens(null, null, 0).getContent().get(0);

            assertThat(dto.produtoNome()).isNull();
            assertThat(dto.produtoCodigo()).isNull();
            assertThat(dto.totalEtapas()).isZero();
            assertThat(dto.progresso()).isZero();
            verifyNoInteractions(roteiroPassoRepository);
        }

        @Test
        @DisplayName("Etapas de ordens diferentes não se misturam")
        void ordensDiferentes() {
            Roteiro roteiro = roteiro(3L);
            OrdemProducao o1 = ordem(1L, roteiro, EstatusOrdemProducao.DISPONIVEL);
            OrdemProducao o2 = ordem(2L, roteiro, EstatusOrdemProducao.DISPONIVEL);
            stubPagina(o1, o2);
            when(ordemSetorRepository.findByOrdemProducaoIdInOrderBySequenciaAsc(List.of(1L, 2L))).thenReturn(List.of(
                    linha(o1, 10, EstatusOrdemProducao.CONCLUIDA, "CORTE"),
                    linha(o2, 10, EstatusOrdemProducao.LIBERADA, "CORTE")));
            when(roteiroPassoRepository.buscarPassosPorRoteiroIds(Set.of(3L)))
                    .thenReturn(List.of(new PassoDoRoteiroDTO(3L, 10, null)));

            List<DashboardOrdemDTO> dtos = service.listarOrdens(null, null, 0).getContent();

            assertThat(dtos.get(0).etapasConcluidas()).isEqualTo(1);
            assertThat(dtos.get(0).status()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            assertThat(dtos.get(1).etapasConcluidas()).isZero();
            assertThat(dtos.get(1).status()).isEqualTo(EstatusOrdemProducao.LIBERADA);
        }

        @Test
        @DisplayName("Erros de banco e inesperados viram falha de validação")
        void erros() {
            when(ordemProducaoRepository.buscarParaDashboard(eq(""), any(), eq(pagina0)))
                    .thenThrow(new DataRetrievalFailureException("x"))
                    .thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.listarOrdens(null, null, 0), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.listarOrdens(null, null, 0), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}
