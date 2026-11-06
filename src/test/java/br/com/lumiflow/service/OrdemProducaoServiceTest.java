package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.DividirEtapaDTO;
import br.com.lumiflow.dto.ordens.DivisaoItemDTO;
import br.com.lumiflow.dto.ordens.OrdemFiltroDTO;
import br.com.lumiflow.dto.ordens.OrdemListagemDTO;
import br.com.lumiflow.dto.ordens.OrdemProducaoDTO;
import br.com.lumiflow.dto.roteiro.PassoDoRoteiroDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.Produto;
import br.com.lumiflow.entity.Roteiro;
import br.com.lumiflow.entity.RoteiroPasso;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import br.com.lumiflow.repository.RefugoRepository;
import br.com.lumiflow.repository.RoteiroRepository;
import br.com.lumiflow.repository.RoteiroPassoRepository;
import br.com.lumiflow.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemProducaoServiceTest {

    @Mock
    private OrdemProducaoRepository ordemProducaoRepository;
    @Mock
    private ProdutoService produtoService;
    @Mock
    private RoteiroPassoRepository roteiroPassoRepository;
    @Mock
    private OrdemSetorRepository ordemSetorRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RoteiroRepository roteiroRepository;
    @Mock
    private RefugoRepository refugoRepository;
    @Spy
    private Clock clock = RelogioTeste.fixoEm(LocalDateTime.of(2025, 6, 15, 9, 30));

    @InjectMocks
    private OrdemProducaoService service;

    private static void assertChave(Runnable acao, String chave) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", chave);
    }

    private Setor setor(Long id) {
        Setor s = new Setor();
        s.setId(id);
        s.setNome("SETOR " + id);
        return s;
    }

    private Roteiro roteiro(Long id) {
        Roteiro r = new Roteiro();
        r.setId(id);
        r.setCodigo("R-" + id);
        return r;
    }

    private OrdemProducao ordem(Long id, Roteiro roteiro) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("PORTA");
        produto.setCodigo("P-01");
        OrdemProducao o = new OrdemProducao();
        o.setId(id);
        o.setNumero("OP-000" + id);
        o.setObservacao("obs");
        o.setProduto(produto);
        o.setRoteiro(roteiro);
        o.setCriadoEm(LocalDateTime.of(2025, 1, 1, 10, 0));
        return o;
    }

    private OrdemSetor linha(Long id, OrdemProducao ordem, int sequencia, int recebida, int produzida,
                             EstatusOrdemProducao status) {
        OrdemSetor os = new OrdemSetor();
        os.setId(id);
        os.setOrdemProducao(ordem);
        os.setSetor(setor(5L));
        os.setSequencia(sequencia);
        os.setQtdRecebida(recebida);
        os.setQtdProduzida(produzida);
        os.setQtdPendente(recebida - produzida);
        os.setStatus(status);
        return os;
    }

    @Nested
    @DisplayName("listarPorSetorIdEFiltros")
    class Listar {

        private final PageRequest pagina0 = PageRequest.of(0, 50);

        @Test
        @DisplayName("Setor nulo é rejeitado")
        void setorNulo() {
            assertChave(() -> service.listarPorSetorIdEFiltros(null, null, 0), AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(ordemSetorRepository);
        }

        @Test
        @DisplayName("Sem filtro usa busca vazia, status em aberto e página de 50")
        void semFiltro() {
            when(ordemSetorRepository.buscarPorSetorIdEFiltros(eq(5L), eq(""), any(), eq(pagina0)))
                    .thenReturn(Page.empty(pagina0));

            service.listarPorSetorIdEFiltros(5L, null, 0);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<EstatusOrdemProducao>> statuses = ArgumentCaptor.forClass(Collection.class);
            verify(ordemSetorRepository).buscarPorSetorIdEFiltros(eq(5L), eq(""), statuses.capture(), eq(pagina0));
            assertThat(statuses.getValue()).containsExactlyInAnyOrder(EstatusOrdemProducao.DISPONIVEL,
                    EstatusOrdemProducao.LIBERADA, EstatusOrdemProducao.EM_ANDAMENTO);
            verifyNoInteractions(roteiroPassoRepository);
        }

        @Test
        @DisplayName("Filtro TODAS inclui concluídas; status específico filtra só ele; status inválido volta ao padrão")
        void filtrosDeStatus() {
            when(ordemSetorRepository.buscarPorSetorIdEFiltros(eq(5L), eq("x"), any(), eq(pagina0)))
                    .thenReturn(Page.empty(pagina0));

            service.listarPorSetorIdEFiltros(5L, new OrdemFiltroDTO(null, " x ", " todas "), 0);
            service.listarPorSetorIdEFiltros(5L, new OrdemFiltroDTO(null, "x", "CONCLUIDA"), 0);
            service.listarPorSetorIdEFiltros(5L, new OrdemFiltroDTO(null, "x", "INEXISTENTE"), 0);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<EstatusOrdemProducao>> statuses = ArgumentCaptor.forClass(Collection.class);
            verify(ordemSetorRepository, Mockito.times(3))
                    .buscarPorSetorIdEFiltros(eq(5L), eq("x"), statuses.capture(), eq(pagina0));
            List<Collection<EstatusOrdemProducao>> valores = statuses.getAllValues();
            assertThat(valores.get(0)).containsExactlyInAnyOrder(EstatusOrdemProducao.values());
            assertThat(valores.get(1)).containsExactly(EstatusOrdemProducao.CONCLUIDA);
            assertThat(valores.get(2)).containsExactlyInAnyOrder(EstatusOrdemProducao.DISPONIVEL,
                    EstatusOrdemProducao.LIBERADA, EstatusOrdemProducao.EM_ANDAMENTO);
        }

        @Test
        @DisplayName("Página negativa é tratada como zero")
        void paginaNegativa() {
            when(ordemSetorRepository.buscarPorSetorIdEFiltros(eq(5L), eq(""), any(), eq(pagina0)))
                    .thenReturn(Page.empty(pagina0));

            service.listarPorSetorIdEFiltros(5L, null, -4);

            verify(ordemSetorRepository).buscarPorSetorIdEFiltros(eq(5L), eq(""), any(), eq(pagina0));
        }

        @Test
        @DisplayName("Monta o DTO com nome da etapa do roteiro, percentual e dados da ordem")
        void montaDto() {
            Roteiro roteiro = roteiro(3L);
            OrdemProducao ordem = ordem(10L, roteiro);
            OrdemSetor os = linha(100L, ordem, 20, 8, 2, EstatusOrdemProducao.EM_ANDAMENTO);
            os.setVariante("LOTE A");
            os.setOrigemRefugo(true);
            when(ordemSetorRepository.buscarPorSetorIdEFiltros(eq(5L), eq(""), any(), eq(pagina0)))
                    .thenReturn(new PageImpl<>(List.of(os), pagina0, 1));
            when(roteiroPassoRepository.buscarPassosPorRoteiroIds(Set.of(3L))).thenReturn(List.of(
                    new PassoDoRoteiroDTO(3L, 10, "SERRA"),
                    new PassoDoRoteiroDTO(3L, 20, "1ª DOBRA"),
                    new PassoDoRoteiroDTO(3L, 20, "DUPLICADA")));

            Page<OrdemListagemDTO> resultado = service.listarPorSetorIdEFiltros(5L, null, 0);

            assertThat(resultado.getContent()).containsExactly(new OrdemListagemDTO(
                    10L, 100L, "OP-00010", "PORTA", "P-01", "1ª DOBRA", "LOTE A", true,
                    8, 2, 25.0, "obs", LocalDateTime.of(2025, 1, 1, 10, 0), EstatusOrdemProducao.EM_ANDAMENTO));
        }

        @Test
        @DisplayName("Etapa sem nome no roteiro e recebido zero resultam em etapa nula e 0%")
        void semNomeDeEtapaERecebidoZero() {
            Roteiro roteiro = roteiro(3L);
            OrdemSetor os = linha(100L, ordem(10L, roteiro), 20, 0, 0, EstatusOrdemProducao.DISPONIVEL);
            when(ordemSetorRepository.buscarPorSetorIdEFiltros(eq(5L), eq(""), any(), eq(pagina0)))
                    .thenReturn(new PageImpl<>(List.of(os), pagina0, 1));
            when(roteiroPassoRepository.buscarPassosPorRoteiroIds(Set.of(3L)))
                    .thenReturn(List.of(new PassoDoRoteiroDTO(3L, 20, null)));

            OrdemListagemDTO dto = service.listarPorSetorIdEFiltros(5L, null, 0).getContent().get(0);

            assertThat(dto.etapaNome()).isNull();
            assertThat(dto.percentual()).isZero();
        }

        @Test
        @DisplayName("Ordem sem roteiro não consulta passos e retorna etapa nula")
        void ordemSemRoteiro() {
            OrdemSetor os = linha(100L, ordem(10L, null), 20, 4, 1, EstatusOrdemProducao.LIBERADA);
            when(ordemSetorRepository.buscarPorSetorIdEFiltros(eq(5L), eq(""), any(), eq(pagina0)))
                    .thenReturn(new PageImpl<>(List.of(os), pagina0, 1));

            OrdemListagemDTO dto = service.listarPorSetorIdEFiltros(5L, null, 0).getContent().get(0);

            assertThat(dto.etapaNome()).isNull();
            verifyNoInteractions(roteiroPassoRepository);
        }

        @Test
        @DisplayName("Erros de banco e inesperados viram falha de validação")
        void erros() {
            when(ordemSetorRepository.buscarPorSetorIdEFiltros(eq(5L), eq(""), any(), eq(pagina0)))
                    .thenThrow(new DataRetrievalFailureException("x"))
                    .thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.listarPorSetorIdEFiltros(5L, null, 0), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.listarPorSetorIdEFiltros(5L, null, 0), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        private final LocalDate data = LocalDate.of(2025, 6, 15);

        private Produto produtoComRoteiro(Roteiro roteiro) {
            Produto p = new Produto();
            p.setId(1L);
            p.setRoteiro(roteiro);
            return p;
        }

        private RoteiroPasso passo(Roteiro roteiro, int sequencia, Setor setor) {
            RoteiroPasso p = new RoteiroPasso();
            p.setRoteiro(roteiro);
            p.setSequencia(sequencia);
            p.setSetor(setor);
            return p;
        }

        @Test
        @DisplayName("DTO ou usuário nulos são rejeitados")
        void entradasNulas() {
            OrdemProducaoDTO dto = new OrdemProducaoDTO(1L, 10, data, null);

            assertChave(() -> service.criar(null, 1L), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.criar(dto, null), AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(ordemProducaoRepository, produtoService);
        }

        @Test
        @DisplayName("Produto sem roteiro não gera ordem")
        void produtoSemRoteiro() {
            when(produtoService.buscarProdutoPorId(1L)).thenReturn(produtoComRoteiro(null));

            assertChave(() -> service.criar(new OrdemProducaoDTO(1L, 10, data, null), 1L),
                    AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE);

            verify(ordemProducaoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Produto inexistente propaga o erro do ProdutoService")
        void produtoInexistente() {
            when(produtoService.buscarProdutoPorId(1L))
                    .thenThrow(new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND));

            assertChave(() -> service.criar(new OrdemProducaoDTO(1L, 10, data, null), 1L),
                    AppMessages.ERROR_PRODUCT_NOTFOUND);
        }


        @Test
        @DisplayName("Roteiro do produto que sumiu antes do bloqueio é tratado como produto sem roteiro")
        void roteiroNaoEncontradoNoBloqueio() {
            Roteiro roteiro = roteiro(3L);
            when(produtoService.buscarProdutoPorId(1L)).thenReturn(produtoComRoteiro(roteiro));
            when(roteiroRepository.findByIdForUpdate(3L)).thenReturn(Optional.empty());

            assertChave(() -> service.criar(new OrdemProducaoDTO(1L, 10, data, null), 1L),
                    AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE);

            verify(ordemProducaoRepository, never()).save(any());
        }
        @Test
        @DisplayName("Roteiro sem passos impede a criação da primeira etapa")
        void roteiroSemPassos() {
            Roteiro roteiro = roteiro(3L);
            when(produtoService.buscarProdutoPorId(1L)).thenReturn(produtoComRoteiro(roteiro));
            when(roteiroRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(roteiro));
            when(ordemProducaoRepository.proximoNumeroSequencial()).thenReturn(1L);
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(3L)).thenReturn(List.of());

            assertChave(() -> service.criar(new OrdemProducaoDTO(1L, 10, data, null), 1L),
                    AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE);

            verify(ordemSetorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cria a ordem DISPONIVEL com número sequencial, roteiro congelado e primeira etapa do roteiro")
        void criaOrdemEPrimeiraEtapa() {
            Roteiro roteiro = roteiro(3L);
            Produto produto = produtoComRoteiro(roteiro);
            Usuario criador = new Usuario();
            criador.setId(7L);
            Setor primeiroSetor = setor(11L);
            when(produtoService.buscarProdutoPorId(1L)).thenReturn(produto);
            when(roteiroRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(roteiro));
            when(ordemProducaoRepository.proximoNumeroSequencial()).thenReturn(5L);
            when(usuarioRepository.getReferenceById(7L)).thenReturn(criador);
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(3L)).thenReturn(List.of(
                    passo(roteiro, 10, primeiroSetor), passo(roteiro, 20, setor(12L))));

            service.criar(new OrdemProducaoDTO(1L, 40, data, "urgente"), 7L);

            ArgumentCaptor<OrdemProducao> ordemCaptor = ArgumentCaptor.forClass(OrdemProducao.class);
            verify(ordemProducaoRepository).save(ordemCaptor.capture());
            OrdemProducao ordem = ordemCaptor.getValue();
            assertThat(ordem.getNumero()).isEqualTo("OP-0005");
            assertThat(ordem.getStatus()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
            assertThat(ordem.getQuantidade()).isEqualTo(40);
            assertThat(ordem.getProduto()).isSameAs(produto);
            assertThat(ordem.getRoteiro()).isSameAs(roteiro);
            assertThat(ordem.getCriadoPor()).isSameAs(criador);
            assertThat(ordem.getDataCriacao()).isEqualTo(data.atStartOfDay());
            assertThat(ordem.getObservacao()).isEqualTo("urgente");

            ArgumentCaptor<OrdemSetor> etapaCaptor = ArgumentCaptor.forClass(OrdemSetor.class);
            verify(ordemSetorRepository).save(etapaCaptor.capture());
            OrdemSetor etapa = etapaCaptor.getValue();
            assertThat(etapa.getOrdemProducao()).isSameAs(ordem);
            assertThat(etapa.getSetor()).isSameAs(primeiroSetor);
            assertThat(etapa.getSequencia()).isEqualTo(10);
            assertThat(etapa.getQtdRecebida()).isEqualTo(40);
            assertThat(etapa.getQtdPendente()).isEqualTo(40);
            assertThat(etapa.getQtdProduzida()).isZero();
            assertThat(etapa.getStatus()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
        }

        @Test
        @DisplayName("Sem data informada usa o momento atual e sequência nula gera OP-0001")
        void semDataENumeroInicial() {
            Roteiro roteiro = roteiro(3L);
            when(produtoService.buscarProdutoPorId(1L)).thenReturn(produtoComRoteiro(roteiro));
            when(roteiroRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(roteiro));
            when(ordemProducaoRepository.proximoNumeroSequencial()).thenReturn(null);
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(3L))
                    .thenReturn(List.of(passo(roteiro, 10, setor(11L))));

            service.criar(new OrdemProducaoDTO(1L, 5, null, null), 7L);

            ArgumentCaptor<OrdemProducao> captor = ArgumentCaptor.forClass(OrdemProducao.class);
            verify(ordemProducaoRepository).save(captor.capture());
            assertThat(captor.getValue().getNumero()).isEqualTo("OP-0001");
            assertThat(captor.getValue().getDataCriacao()).isEqualTo(LocalDateTime.of(2025, 6, 15, 9, 30));
        }

        @Test
        @DisplayName("Números acima de 9999 não são truncados")
        void numeroGrande() {
            Roteiro roteiro = roteiro(3L);
            when(produtoService.buscarProdutoPorId(1L)).thenReturn(produtoComRoteiro(roteiro));
            when(roteiroRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(roteiro));
            when(ordemProducaoRepository.proximoNumeroSequencial()).thenReturn(12345L);
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(3L))
                    .thenReturn(List.of(passo(roteiro, 10, setor(11L))));

            service.criar(new OrdemProducaoDTO(1L, 5, data, null), 7L);

            ArgumentCaptor<OrdemProducao> captor = ArgumentCaptor.forClass(OrdemProducao.class);
            verify(ordemProducaoRepository).save(captor.capture());
            assertThat(captor.getValue().getNumero()).isEqualTo("OP-12345");
        }

        @Test
        @DisplayName("Erros de banco e inesperados viram falha de validação")
        void erros() {
            when(produtoService.buscarProdutoPorId(1L))
                    .thenThrow(new DataRetrievalFailureException("x"))
                    .thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.criar(new OrdemProducaoDTO(1L, 5, data, null), 7L),
                    AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.criar(new OrdemProducaoDTO(1L, 5, data, null), 7L),
                    AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("liberarOrdem")
    class Liberar {

        @Test
        @DisplayName("Id nulo ou inexistente resulta em etapa não encontrada")
        void naoEncontrada() {
            when(ordemSetorRepository.findById(9L)).thenReturn(Optional.empty());

            assertChave(() -> service.liberarOrdem(null), AppMessages.ERROR_ORDER_SECTOR_NOTFOUND);
            assertChave(() -> service.liberarOrdem(9L), AppMessages.ERROR_ORDER_SECTOR_NOTFOUND);
        }

        @Test
        @DisplayName("Etapa DISPONIVEL passa para LIBERADA e é salva")
        void liberaDisponivel() {
            OrdemSetor os = linha(1L, ordem(10L, null), 10, 5, 0, EstatusOrdemProducao.DISPONIVEL);
            when(ordemSetorRepository.findById(1L)).thenReturn(Optional.of(os));

            service.liberarOrdem(1L);

            assertThat(os.getStatus()).isEqualTo(EstatusOrdemProducao.LIBERADA);
            verify(ordemSetorRepository).save(os);
        }

        @Test
        @DisplayName("Linha de refugo DISPONIVEL também é liberada: é a liberação que habilita registrarRetrabalho")
        void liberaLinhaDeRefugo() {
            OrdemSetor os = linha(1L, ordem(10L, null), 10, 5, 0, EstatusOrdemProducao.DISPONIVEL);
            os.setOrigemRefugo(true);
            when(ordemSetorRepository.findById(1L)).thenReturn(Optional.of(os));

            service.liberarOrdem(1L);

            assertThat(os.getStatus()).isEqualTo(EstatusOrdemProducao.LIBERADA);
            verify(ordemSetorRepository).save(os);
        }

        @ParameterizedTest
        @EnumSource(value = EstatusOrdemProducao.class, names = "DISPONIVEL", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("Etapa em qualquer status diferente de DISPONIVEL não pode ser liberada")
        void naoLiberaOutrosStatus(EstatusOrdemProducao status) {
            OrdemSetor os = linha(1L, ordem(10L, null), 10, 5, 0, status);
            when(ordemSetorRepository.findById(1L)).thenReturn(Optional.of(os));

            assertChave(() -> service.liberarOrdem(1L), AppMessages.ERROR_ORDER_NOT_AVAILABLE);

            assertThat(os.getStatus()).isEqualTo(status);
            verify(ordemSetorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Conflito de versão (lock otimista) é propagado para o handler global")
        void lockOtimistaEhPropagado() {
            OrdemSetor os = linha(1L, ordem(10L, null), 10, 5, 0, EstatusOrdemProducao.DISPONIVEL);
            ObjectOptimisticLockingFailureException conflito = new ObjectOptimisticLockingFailureException(OrdemSetor.class, 1L);
            when(ordemSetorRepository.findById(1L)).thenReturn(Optional.of(os));
            when(ordemSetorRepository.save(os)).thenThrow(conflito);

            assertThatThrownBy(() -> service.liberarOrdem(1L)).isSameAs(conflito);
        }

        @Test
        @DisplayName("Erros de banco e inesperados viram falha de validação")
        void erros() {
            when(ordemSetorRepository.findById(1L))
                    .thenThrow(new DataRetrievalFailureException("x"))
                    .thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.liberarOrdem(1L), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.liberarOrdem(1L), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("pausarEtapa")
    class Pausar {

        @Test
        @DisplayName("Id nulo ou inexistente resulta em etapa não encontrada")
        void naoEncontrada() {
            when(ordemSetorRepository.findById(9L)).thenReturn(Optional.empty());

            assertChave(() -> service.pausarEtapa(null), AppMessages.ERROR_ORDER_SECTOR_NOTFOUND);
            assertChave(() -> service.pausarEtapa(9L), AppMessages.ERROR_ORDER_SECTOR_NOTFOUND);
        }

        @ParameterizedTest
        @EnumSource(value = EstatusOrdemProducao.class, names = {"LIBERADA", "EM_ANDAMENTO"})
        @DisplayName("Etapa LIBERADA ou EM_ANDAMENTO volta para DISPONIVEL")
        void pausa(EstatusOrdemProducao status) {
            OrdemSetor os = linha(1L, ordem(10L, null), 10, 5, 2, status);
            when(ordemSetorRepository.findById(1L)).thenReturn(Optional.of(os));

            service.pausarEtapa(1L);

            assertThat(os.getStatus()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
            assertThat(os.getQtdProduzida()).isEqualTo(2);
            verify(ordemSetorRepository).save(os);
        }

        @ParameterizedTest
        @EnumSource(value = EstatusOrdemProducao.class, names = {"DISPONIVEL", "CONCLUIDA"})
        @DisplayName("Etapa DISPONIVEL ou CONCLUIDA não pode ser pausada")
        void naoPausa(EstatusOrdemProducao status) {
            OrdemSetor os = linha(1L, ordem(10L, null), 10, 5, 0, status);
            when(ordemSetorRepository.findById(1L)).thenReturn(Optional.of(os));

            assertChave(() -> service.pausarEtapa(1L), AppMessages.ERROR_ORDER_NOT_PAUSABLE);

            verify(ordemSetorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Conflito de versão é propagado e erros de banco viram falha de validação")
        void conflitoEErros() {
            OrdemSetor os = linha(1L, ordem(10L, null), 10, 5, 0, EstatusOrdemProducao.LIBERADA);
            ObjectOptimisticLockingFailureException conflito = new ObjectOptimisticLockingFailureException(OrdemSetor.class, 1L);
            when(ordemSetorRepository.findById(1L)).thenReturn(Optional.of(os));
            when(ordemSetorRepository.save(os)).thenThrow(conflito).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.pausarEtapa(1L)).isSameAs(conflito);
            os.setStatus(EstatusOrdemProducao.LIBERADA); // o service já alterou o status em memória na 1ª chamada
            assertChave(() -> service.pausarEtapa(1L), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("dividirEtapa")
    class Dividir {

        private OrdemSetor original;
        private OrdemProducao ordem;

        private DividirEtapaDTO divisao(DivisaoItemDTO... itens) {
            return new DividirEtapaDTO(List.of(itens));
        }

        private void prepararOriginal(int recebida, int produzida, EstatusOrdemProducao status) {
            ordem = ordem(10L, null);
            original = linha(1L, ordem, 20, recebida, produzida, status);
            when(ordemSetorRepository.findById(1L)).thenReturn(Optional.of(original));
        }

        @Test
        @DisplayName("Entradas nulas são rejeitadas")
        void entradasNulas() {
            assertChave(() -> service.dividirEtapa(null, divisao(new DivisaoItemDTO("A", 1))),
                    AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.dividirEtapa(1L, null), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.dividirEtapa(1L, new DividirEtapaDTO(null)), AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(ordemSetorRepository);
        }

        @Test
        @DisplayName("Etapa inexistente é rejeitada")
        void inexistente() {
            when(ordemSetorRepository.findById(1L)).thenReturn(Optional.empty());

            assertChave(() -> service.dividirEtapa(1L, divisao(new DivisaoItemDTO("A", 1))),
                    AppMessages.ERROR_ORDER_SECTOR_NOTFOUND);
        }

        @ParameterizedTest
        @EnumSource(value = EstatusOrdemProducao.class, names = "DISPONIVEL", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("Somente etapas DISPONIVEL podem ser divididas")
        void somenteDisponivel(EstatusOrdemProducao status) {
            prepararOriginal(10, 0, status);

            assertChave(() -> service.dividirEtapa(1L, divisao(new DivisaoItemDTO("A", 1))),
                    AppMessages.ERROR_ORDER_NOT_AVAILABLE);

            verify(ordemSetorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Linha originada de refugo não pode ser dividida")
        void linhaDeRefugo() {
            prepararOriginal(10, 0, EstatusOrdemProducao.DISPONIVEL);
            original.setOrigemRefugo(true);

            assertChave(() -> service.dividirEtapa(1L, divisao(new DivisaoItemDTO("A", 1))),
                    AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("Etapa que já teve produção não pode ser dividida")
        void jaIniciada() {
            prepararOriginal(10, 3, EstatusOrdemProducao.DISPONIVEL);

            assertChave(() -> service.dividirEtapa(1L, divisao(new DivisaoItemDTO("A", 1))),
                    AppMessages.ERROR_DIVISION_ALREADY_STARTED);
        }

        @Test
        @DisplayName("Soma das divisões acima do pendente é rejeitada")
        void somaAcimaDoPendente() {
            prepararOriginal(10, 0, EstatusOrdemProducao.DISPONIVEL);

            assertChave(() -> service.dividirEtapa(1L,
                            divisao(new DivisaoItemDTO("A", 6), new DivisaoItemDTO("B", 5))),
                    AppMessages.ERROR_DIVISION_SUM_MISMATCH);

            verify(ordemSetorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Variantes repetidas na mesma divisão (ignorando caixa e espaços) são rejeitadas")
        void varianteRepetidaNaDivisao() {
            prepararOriginal(10, 0, EstatusOrdemProducao.DISPONIVEL);

            assertChave(() -> service.dividirEtapa(1L,
                            divisao(new DivisaoItemDTO("lote a", 3), new DivisaoItemDTO(" LOTE A ", 3))),
                    AppMessages.ERROR_DIVISION_VARIANT_DUPLICATE);

            verify(ordemSetorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Variante já existente na ordem e sequência é rejeitada")
        void varianteJaExisteNaOrdem() {
            prepararOriginal(10, 0, EstatusOrdemProducao.DISPONIVEL);
            when(ordemSetorRepository.findByOrdemProducaoIdAndSequenciaAndVariante(10L, 20, "LOTE A"))
                    .thenReturn(Optional.of(new OrdemSetor()));

            assertChave(() -> service.dividirEtapa(1L, divisao(new DivisaoItemDTO("lote a", 3))),
                    AppMessages.ERROR_DIVISION_VARIANT_DUPLICATE);

            verify(ordemSetorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Divisão parcial cria as novas etapas e mantém a sobra na linha original")
        void divisaoParcial() {
            prepararOriginal(10, 0, EstatusOrdemProducao.DISPONIVEL);

            service.dividirEtapa(1L, divisao(new DivisaoItemDTO(" lote a ", 3), new DivisaoItemDTO("lote b", 4)));

            ArgumentCaptor<OrdemSetor> captor = ArgumentCaptor.forClass(OrdemSetor.class);
            verify(ordemSetorRepository, Mockito.times(3)).save(captor.capture());
            List<OrdemSetor> salvas = captor.getAllValues();
            assertThat(salvas.get(0).getVariante()).isEqualTo("LOTE A");
            assertThat(salvas.get(0).getQtdRecebida()).isEqualTo(3);
            assertThat(salvas.get(0).getQtdPendente()).isEqualTo(3);
            assertThat(salvas.get(0).getQtdProduzida()).isZero();
            assertThat(salvas.get(0).getStatus()).isEqualTo(EstatusOrdemProducao.DISPONIVEL);
            assertThat(salvas.get(0).getOrdemProducao()).isSameAs(ordem);
            assertThat(salvas.get(0).getSetor()).isSameAs(original.getSetor());
            assertThat(salvas.get(0).getSequencia()).isEqualTo(20);
            assertThat(salvas.get(1).getVariante()).isEqualTo("LOTE B");
            assertThat(salvas.get(1).getQtdRecebida()).isEqualTo(4);
            assertThat(salvas.get(2)).isSameAs(original);
            assertThat(original.getQtdRecebida()).isEqualTo(3);
            assertThat(original.getQtdPendente()).isEqualTo(3);
            verify(ordemSetorRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Divisão total remove a linha original")
        void divisaoTotalRemoveOriginal() {
            prepararOriginal(10, 0, EstatusOrdemProducao.DISPONIVEL);

            service.dividirEtapa(1L, divisao(new DivisaoItemDTO("A", 6), new DivisaoItemDTO("B", 4)));

            verify(ordemSetorRepository, Mockito.times(2)).save(any(OrdemSetor.class));
            verify(ordemSetorRepository).delete(original);
            verify(ordemSetorRepository, never()).save(original);
        }


        @Test
        @DisplayName("Divisão total de linha que já tem refugo não apaga a original: zera e conclui")
        void divisaoTotalComRefugoPreservaOriginal() {
            prepararOriginal(10, 0, EstatusOrdemProducao.DISPONIVEL);
            when(refugoRepository.existsByOrdemSetorId(1L)).thenReturn(true);

            service.dividirEtapa(1L, divisao(new DivisaoItemDTO("A", 6), new DivisaoItemDTO("B", 4)));

            verify(ordemSetorRepository, never()).delete(any());
            verify(ordemSetorRepository).save(original);
            assertThat(original.getQtdPendente()).isZero();
            assertThat(original.getQtdRecebida()).isZero();
            assertThat(original.getStatus()).isEqualTo(EstatusOrdemProducao.CONCLUIDA);
            assertThat(original.getFim()).isEqualTo(LocalDateTime.of(2025, 6, 15, 9, 30));
        }
        @Test
        @DisplayName("Conflito de versão é propagado e erro de banco vira falha de validação")
        void conflitoEErros() {
            prepararOriginal(10, 0, EstatusOrdemProducao.DISPONIVEL);
            ObjectOptimisticLockingFailureException conflito = new ObjectOptimisticLockingFailureException(OrdemSetor.class, 1L);
            when(ordemSetorRepository.save(any(OrdemSetor.class)))
                    .thenThrow(conflito)
                    .thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.dividirEtapa(1L, divisao(new DivisaoItemDTO("A", 2)))).isSameAs(conflito);
            assertChave(() -> service.dividirEtapa(1L, divisao(new DivisaoItemDTO("A", 2))),
                    AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}
