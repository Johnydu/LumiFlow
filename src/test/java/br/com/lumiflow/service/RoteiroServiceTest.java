package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.comum.PaginaDTO;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.roteiro.PassoRoteiroDTO;
import br.com.lumiflow.dto.roteiro.ProdutoVinculoDTO;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroListagemDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.entity.Produto;
import br.com.lumiflow.entity.Roteiro;
import br.com.lumiflow.entity.RoteiroPasso;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.ProdutoRepository;
import br.com.lumiflow.repository.RoteiroPassoRepository;
import br.com.lumiflow.repository.RoteiroRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoteiroServiceTest {

    @Mock
    private RoteiroRepository roteiroRepository;
    @Mock
    private RoteiroPassoRepository roteiroPassoRepository;
    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private SetorService setorService;
    @Mock
    private EtapaSetorService etapaSetorService;
    @Mock
    private OrdemProducaoRepository ordemProducaoRepository;

    @InjectMocks
    private RoteiroService service;

    private static void assertChave(Runnable acao, String chave) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", chave);
    }

    private Roteiro roteiro(Long id, String codigo) {
        Roteiro r = new Roteiro();
        r.setId(id);
        r.setCodigo(codigo);
        r.setDescricao("DESC " + codigo);
        return r;
    }

    private Setor setor(Long id, String nome) {
        Setor s = new Setor();
        s.setId(id);
        s.setNome(nome);
        return s;
    }

    private EtapaSetor etapa(Long id, String nome, Setor setor) {
        EtapaSetor e = new EtapaSetor();
        e.setId(id);
        e.setNome(nome);
        e.setOrdem(1);
        e.setSetor(setor);
        return e;
    }

    private RoteiroPasso passo(Roteiro roteiro, int sequencia, Setor setor, EtapaSetor etapa) {
        RoteiroPasso p = new RoteiroPasso();
        p.setRoteiro(roteiro);
        p.setSequencia(sequencia);
        p.setSetor(setor);
        p.setEtapaSetor(etapa);
        return p;
    }

    private Produto produto(Long id, String codigo, Roteiro roteiro) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome("PROD " + id);
        p.setCodigo(codigo);
        p.setRoteiro(roteiro);
        return p;
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("listarRoteiros sem resultados retorna vazio sem consultar passos nem produtos")
        void listarVazio() {
            when(roteiroRepository.buscar(null)).thenReturn(List.of());

            assertThat(service.listarRoteiros("  ")).isEmpty();

            verifyNoInteractions(roteiroPassoRepository, produtoRepository);
        }

        @Test
        @DisplayName("listarRoteiros monta etapas descritas, quantidade de passos e de produtos")
        void listarCompleto() {
            Roteiro r1 = roteiro(1L, "R-1");
            Roteiro r2 = roteiro(2L, "R-2");
            Setor corte = setor(10L, "CORTE");
            Setor dobra = setor(11L, "DOBRA");
            when(roteiroRepository.buscar("r")).thenReturn(List.of(r1, r2));
            when(roteiroPassoRepository.buscarPassosDosRoteiros(List.of(1L, 2L))).thenReturn(List.of(
                    passo(r1, 10, corte, null),
                    passo(r1, 20, dobra, etapa(5L, "1ª DOBRA", dobra))));
            when(produtoRepository.contarPorRoteiros(List.of(1L, 2L)))
                    .thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));

            List<RoteiroListagemDTO> lista = service.listarRoteiros(" r ");

            assertThat(lista).containsExactly(
                    new RoteiroListagemDTO(1L, "R-1", "DESC R-1", 2, List.of("CORTE", "DOBRA (1ª DOBRA)"), 3L),
                    new RoteiroListagemDTO(2L, "R-2", "DESC R-2", 0, List.of(), 0L));
        }

        @Test
        @DisplayName("buscarPorId monta o DTO com os passos em ordem")
        void buscarPorId() {
            Roteiro r = roteiro(1L, "R-1");
            Setor corte = setor(10L, "CORTE");
            Setor dobra = setor(11L, "DOBRA");
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(r));
            when(roteiroPassoRepository.buscarPassosComSetorEEtapa(1L)).thenReturn(List.of(
                    passo(r, 10, corte, null), passo(r, 20, dobra, etapa(5L, "1ª", dobra))));

            RoteiroDTO dto = service.buscarPorId(1L);

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.codigo()).isEqualTo("R-1");
            assertThat(dto.passos()).containsExactly(new PassoRoteiroDTO(10L, null), new PassoRoteiroDTO(11L, 5L));
        }

        @Test
        @DisplayName("buscarPorId com roteiro inexistente lança roteiro não encontrado")
        void buscarPorIdInexistente() {
            when(roteiroRepository.findById(9L)).thenReturn(Optional.empty());

            assertChave(() -> service.buscarPorId(9L), AppMessages.ERROR_ROUTE_NOTFOUND);
        }

        @Test
        @DisplayName("buscarProdutosParaVinculo (todos) marca quais já estão vinculados a este roteiro")
        void produtosParaVinculoTodos() {
            Roteiro r = roteiro(1L, "R-1");
            Roteiro outro = roteiro(2L, "R-2");
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(r));
            when(produtoRepository.buscarPorTermo("p", PageRequest.of(0, 20)))
                    .thenReturn(new PageImpl<>(List.of(
                            produto(1L, "A", r), produto(2L, "B", outro), produto(3L, "C", null)),
                            PageRequest.of(0, 20), 3));

            PaginaDTO<ProdutoVinculoDTO> pagina = service.buscarProdutosParaVinculo(1L, " p ", false, 0);

            assertThat(pagina.itens()).extracting(ProdutoVinculoDTO::vinculadoAqui).containsExactly(true, false, false);
            assertThat(pagina.itens()).extracting(ProdutoVinculoDTO::roteiroCodigo).containsExactly("R-1", "R-2", null);
            assertThat(pagina.temMais()).isFalse();
            assertThat(pagina.total()).isEqualTo(3);
        }

        @Test
        @DisplayName("buscarProdutosParaVinculo (somente vinculados) usa a consulta de vinculados e normaliza página/termo")
        void produtosParaVinculoSomenteVinculados() {
            Roteiro r = roteiro(1L, "R-1");
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(r));
            when(produtoRepository.buscarVinculados(1L, "", PageRequest.of(0, 20)))
                    .thenReturn(new PageImpl<>(List.of(produto(1L, "A", r)), PageRequest.of(0, 20), 1));

            PaginaDTO<ProdutoVinculoDTO> pagina = service.buscarProdutosParaVinculo(1L, null, true, -2);

            assertThat(pagina.itens()).hasSize(1);
            verify(produtoRepository, never()).buscarPorTermo(any(), any());
        }

        @Test
        @DisplayName("buscarProdutosParaVinculo exige roteiro existente")
        void produtosParaVinculoRoteiroInexistente() {
            when(roteiroRepository.findById(1L)).thenReturn(Optional.empty());

            assertChave(() -> service.buscarProdutosParaVinculo(1L, "", false, 0), AppMessages.ERROR_ROUTE_NOTFOUND);
        }

        @Test
        @DisplayName("buscarFluxoPorProdutoId retorna vazio para id nulo e descreve passos para id válido")
        void fluxoPorProduto() {
            Roteiro r = roteiro(1L, "R-1");
            Setor dobra = setor(11L, "DOBRA");
            when(roteiroPassoRepository.buscarPorProdutoId(7L)).thenReturn(List.of(
                    passo(r, 10, setor(10L, "CORTE"), null), passo(r, 20, dobra, etapa(5L, "2ª", dobra))));

            assertThat(service.buscarFluxoPorProdutoId(null)).isEmpty();
            assertThat(service.buscarFluxoPorProdutoId(7L)).containsExactly("CORTE", "DOBRA (2ª)");
        }

        @Test
        @DisplayName("buscarEtapasDoSetorNoRoteiro retorna apenas etapas do setor informado")
        void etapasDoSetor() {
            Roteiro r = roteiro(1L, "R-1");
            Setor dobra = setor(11L, "DOBRA");
            Setor corte = setor(10L, "CORTE");
            EtapaSetor primeira = etapa(5L, "1ª DOBRA", dobra);
            primeira.setOrdem(1);
            when(roteiroPassoRepository.buscarPassosComSetorEEtapa(1L)).thenReturn(List.of(
                    passo(r, 10, corte, etapa(4L, "SERRA", corte)),
                    passo(r, 20, dobra, primeira),
                    passo(r, 30, dobra, null)));

            List<EtapaSetorDTO> etapas = service.buscarEtapasDoSetorNoRoteiro(1L, 11L);

            assertThat(etapas).containsExactly(new EtapaSetorDTO(5L, "1ª DOBRA", 1));
        }
    }

    @Nested
    @DisplayName("criar")
    class Criar {

        private RoteiroDTO dto(String codigo, String descricao, List<PassoRoteiroDTO> passos) {
            return new RoteiroDTO(null, codigo, descricao, passos);
        }

        @Test
        @DisplayName("Código nulo ou em branco é rejeitado")
        void codigoInvalido() {
            assertChave(() -> service.criar(dto(null, null, List.of(new PassoRoteiroDTO(1L, null)))),
                    AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.criar(dto("  ", null, List.of(new PassoRoteiroDTO(1L, null)))),
                    AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(roteiroRepository);
        }

        @Test
        @DisplayName("Código duplicado (ignorando caixa) é rejeitado")
        void codigoDuplicado() {
            when(roteiroRepository.existsByCodigoIgnoreCase("R-1")).thenReturn(true);

            assertChave(() -> service.criar(dto(" r-1 ", null, List.of(new PassoRoteiroDTO(1L, null)))),
                    AppMessages.ERROR_ROUTE_CODE_DUPLICATE);

            verify(roteiroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cria roteiro com código em maiúsculas, descrição aparada e passos de 10 em 10")
        void criaComPassosSequenciados() {
            Setor corte = setor(10L, "CORTE");
            Setor dobra = setor(11L, "DOBRA");
            EtapaSetor etapaDobra = etapa(5L, "1ª", dobra);
            when(roteiroRepository.existsByCodigoIgnoreCase("R-9")).thenReturn(false);
            when(setorService.buscarSetorPorId(10L)).thenReturn(corte);
            when(setorService.buscarSetorPorId(11L)).thenReturn(dobra);
            when(etapaSetorService.buscarPorId(5L)).thenReturn(etapaDobra);

            service.criar(dto(" r-9 ", "  Corte e dobra ",
                    List.of(new PassoRoteiroDTO(10L, null), new PassoRoteiroDTO(11L, 5L))));

            ArgumentCaptor<Roteiro> roteiroCaptor = ArgumentCaptor.forClass(Roteiro.class);
            verify(roteiroRepository).save(roteiroCaptor.capture());
            assertThat(roteiroCaptor.getValue().getCodigo()).isEqualTo("R-9");
            assertThat(roteiroCaptor.getValue().getDescricao()).isEqualTo("Corte e dobra");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RoteiroPasso>> passosCaptor = ArgumentCaptor.forClass(List.class);
            verify(roteiroPassoRepository).saveAll(passosCaptor.capture());
            List<RoteiroPasso> passos = passosCaptor.getValue();
            assertThat(passos).extracting(RoteiroPasso::getSequencia).containsExactly(10, 20);
            assertThat(passos).extracting(RoteiroPasso::getSetor).containsExactly(corte, dobra);
            assertThat(passos).extracting(RoteiroPasso::getEtapaSetor).containsExactly(null, etapaDobra);
            assertThat(passos).allMatch(p -> p.getRoteiro() == roteiroCaptor.getValue());
        }

        @Test
        @DisplayName("Descrição em branco é gravada como nulo")
        void descricaoEmBranco() {
            Setor corte = setor(10L, "CORTE");
            when(roteiroRepository.existsByCodigoIgnoreCase("R-9")).thenReturn(false);
            when(setorService.buscarSetorPorId(10L)).thenReturn(corte);

            service.criar(dto("r-9", "   ", List.of(new PassoRoteiroDTO(10L, null))));

            ArgumentCaptor<Roteiro> captor = ArgumentCaptor.forClass(Roteiro.class);
            verify(roteiroRepository).save(captor.capture());
            assertThat(captor.getValue().getDescricao()).isNull();
        }

        @Test
        @DisplayName("Sem passos (nulo ou vazio) é rejeitado")
        void semPassos() {
            when(roteiroRepository.existsByCodigoIgnoreCase("R-9")).thenReturn(false);

            assertChave(() -> service.criar(dto("r-9", null, null)), AppMessages.ERROR_ROUTE_STEP_REQUIRED);
            assertChave(() -> service.criar(dto("r-9", null, List.of())), AppMessages.ERROR_ROUTE_STEP_REQUIRED);

            verify(roteiroPassoRepository, never()).saveAll(anyCollection());
        }

        @Test
        @DisplayName("Passo nulo ou sem setor é rejeitado")
        void passoSemSetor() {
            when(roteiroRepository.existsByCodigoIgnoreCase("R-9")).thenReturn(false);

            assertChave(() -> service.criar(dto("r-9", null, java.util.Arrays.asList(new PassoRoteiroDTO(null, null)))),
                    AppMessages.ERROR_ROUTE_STEP_REQUIRED);
            assertChave(() -> service.criar(dto("r-9", null, java.util.Arrays.asList((PassoRoteiroDTO) null))),
                    AppMessages.ERROR_ROUTE_STEP_REQUIRED);
        }

        @Test
        @DisplayName("Etapa inexistente é rejeitada")
        void etapaInexistente() {
            when(roteiroRepository.existsByCodigoIgnoreCase("R-9")).thenReturn(false);
            when(setorService.buscarSetorPorId(11L)).thenReturn(setor(11L, "DOBRA"));
            when(etapaSetorService.buscarPorId(99L)).thenReturn(null);

            assertChave(() -> service.criar(dto("r-9", null, List.of(new PassoRoteiroDTO(11L, 99L)))),
                    AppMessages.ERROR_ROUTE_STEP_NOTFOUND);

            verify(roteiroPassoRepository, never()).saveAll(anyCollection());
        }

        @Test
        @DisplayName("Etapa que pertence a outro setor é rejeitada")
        void etapaDeOutroSetor() {
            Setor dobra = setor(11L, "DOBRA");
            Setor corte = setor(10L, "CORTE");
            when(roteiroRepository.existsByCodigoIgnoreCase("R-9")).thenReturn(false);
            when(setorService.buscarSetorPorId(11L)).thenReturn(dobra);
            when(etapaSetorService.buscarPorId(4L)).thenReturn(etapa(4L, "SERRA", corte));

            assertChave(() -> service.criar(dto("r-9", null, List.of(new PassoRoteiroDTO(11L, 4L)))),
                    AppMessages.ERROR_VALIDATION_FAILED);

            verify(roteiroPassoRepository, never()).saveAll(anyCollection());
        }
    }

    @Nested
    @DisplayName("atualizar")
    class Atualizar {

        private final Setor corte = setor(10L, "CORTE");
        private final Setor dobra = setor(11L, "DOBRA");

        @Test
        @DisplayName("Roteiro inexistente é rejeitado")
        void inexistente() {
            when(roteiroRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

            assertChave(() -> service.atualizar(1L, new RoteiroDTO(1L, "R-1", null, List.of())),
                    AppMessages.ERROR_ROUTE_NOTFOUND);
        }


        @Test
        @DisplayName("Carrega o roteiro com lock pessimista (nunca pelo findById comum)")
        void usaLockPessimista() {
            when(roteiroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(roteiro(1L, "R-1")));
            when(roteiroRepository.existsByCodigoIgnoreCaseAndIdNot("R-1", 1L)).thenReturn(true);

            assertChave(() -> service.atualizar(1L, new RoteiroDTO(1L, "R-1", null, List.of())),
                    AppMessages.ERROR_ROUTE_CODE_DUPLICATE);

            verify(roteiroRepository).findByIdForUpdate(1L);
            verify(roteiroRepository, never()).findById(any());
        }
        @Test
        @DisplayName("Código usado por outro roteiro é rejeitado")
        void codigoDuplicado() {
            when(roteiroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(roteiro(1L, "R-1")));
            when(roteiroRepository.existsByCodigoIgnoreCaseAndIdNot("R-2", 1L)).thenReturn(true);

            assertChave(() -> service.atualizar(1L, new RoteiroDTO(1L, "r-2", null, List.of())),
                    AppMessages.ERROR_ROUTE_CODE_DUPLICATE);
        }

        @Test
        @DisplayName("Mudança só de código/descrição não toca nos passos nem verifica uso em ordens")
        void mudancaNaoEstrutural() {
            Roteiro r = roteiro(1L, "R-1");
            when(roteiroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(r));
            when(roteiroRepository.existsByCodigoIgnoreCaseAndIdNot("R-1B", 1L)).thenReturn(false);
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(1L))
                    .thenReturn(List.of(passo(r, 10, corte, null)));

            service.atualizar(1L, new RoteiroDTO(1L, " r-1b ", "  nova ", List.of(new PassoRoteiroDTO(10L, null))));

            assertThat(r.getCodigo()).isEqualTo("R-1B");
            assertThat(r.getDescricao()).isEqualTo("nova");
            verify(roteiroPassoRepository, never()).deletarPorRoteiroId(any());
            verify(roteiroPassoRepository, never()).saveAll(anyCollection());
            verifyNoInteractions(ordemProducaoRepository);
        }

        @Test
        @DisplayName("Mudança estrutural com roteiro já usado em ordens é rejeitada")
        void estruturaComRoteiroEmUso() {
            Roteiro r = roteiro(1L, "R-1");
            when(roteiroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(r));
            when(roteiroRepository.existsByCodigoIgnoreCaseAndIdNot("R-1", 1L)).thenReturn(false);
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(1L))
                    .thenReturn(List.of(passo(r, 10, corte, null)));
            when(ordemProducaoRepository.existsByRoteiroId(1L)).thenReturn(true);

            assertChave(() -> service.atualizar(1L, new RoteiroDTO(1L, "R-1", null,
                            List.of(new PassoRoteiroDTO(10L, null), new PassoRoteiroDTO(11L, null)))),
                    AppMessages.ERROR_ROUTE_IN_USE);

            verify(roteiroPassoRepository, never()).deletarPorRoteiroId(any());
        }

        @Test
        @DisplayName("Mudança estrutural sem ordens remove os passos antigos e grava os novos (nessa ordem)")
        void estruturaSubstituiPassos() {
            Roteiro r = roteiro(1L, "R-1");
            when(roteiroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(r));
            when(roteiroRepository.existsByCodigoIgnoreCaseAndIdNot("R-1", 1L)).thenReturn(false);
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(1L))
                    .thenReturn(List.of(passo(r, 10, corte, null)));
            when(ordemProducaoRepository.existsByRoteiroId(1L)).thenReturn(false);
            when(setorService.buscarSetorPorId(11L)).thenReturn(dobra);

            service.atualizar(1L, new RoteiroDTO(1L, "R-1", null, List.of(new PassoRoteiroDTO(11L, null))));

            InOrder ordem = Mockito.inOrder(roteiroPassoRepository);
            ordem.verify(roteiroPassoRepository).deletarPorRoteiroId(1L);
            ordem.verify(roteiroPassoRepository).saveAll(anyCollection());
        }

        @Test
        @DisplayName("Trocar a etapa do mesmo setor também conta como mudança estrutural")
        void trocaDeEtapaEhEstrutural() {
            Roteiro r = roteiro(1L, "R-1");
            when(roteiroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(r));
            when(roteiroRepository.existsByCodigoIgnoreCaseAndIdNot("R-1", 1L)).thenReturn(false);
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(1L))
                    .thenReturn(List.of(passo(r, 10, dobra, etapa(5L, "1ª", dobra))));
            when(ordemProducaoRepository.existsByRoteiroId(1L)).thenReturn(true);

            assertChave(() -> service.atualizar(1L, new RoteiroDTO(1L, "R-1", null, List.of(new PassoRoteiroDTO(11L, 6L)))),
                    AppMessages.ERROR_ROUTE_IN_USE);
        }
    }

    @Nested
    @DisplayName("duplicar")
    class Duplicar {

        @Test
        @DisplayName("Roteiro de origem inexistente é rejeitado")
        void origemInexistente() {
            when(roteiroRepository.findById(1L)).thenReturn(Optional.empty());

            assertChave(() -> service.duplicar(1L, "R-2"), AppMessages.ERROR_ROUTE_NOTFOUND);
        }

        @Test
        @DisplayName("Novo código duplicado é rejeitado")
        void codigoDuplicado() {
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(roteiro(1L, "R-1")));
            when(roteiroRepository.existsByCodigoIgnoreCase("R-2")).thenReturn(true);

            assertChave(() -> service.duplicar(1L, "r-2"), AppMessages.ERROR_ROUTE_CODE_DUPLICATE);

            verify(roteiroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Copia descrição e todos os passos mantendo sequência, setor e etapa")
        void copiaPassos() {
            Roteiro origem = roteiro(1L, "R-1");
            Setor dobra = setor(11L, "DOBRA");
            EtapaSetor etapa = etapa(5L, "1ª", dobra);
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(origem));
            when(roteiroRepository.existsByCodigoIgnoreCase("R-2")).thenReturn(false);
            when(roteiroPassoRepository.findByRoteiroIdOrderBySequenciaAsc(1L)).thenReturn(List.of(
                    passo(origem, 10, setor(10L, "CORTE"), null), passo(origem, 20, dobra, etapa)));

            service.duplicar(1L, " r-2 ");

            ArgumentCaptor<Roteiro> copiaCaptor = ArgumentCaptor.forClass(Roteiro.class);
            verify(roteiroRepository).save(copiaCaptor.capture());
            assertThat(copiaCaptor.getValue().getCodigo()).isEqualTo("R-2");
            assertThat(copiaCaptor.getValue().getDescricao()).isEqualTo("DESC R-1");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<RoteiroPasso>> passosCaptor = ArgumentCaptor.forClass(List.class);
            verify(roteiroPassoRepository).saveAll(passosCaptor.capture());
            assertThat(passosCaptor.getValue()).extracting(RoteiroPasso::getSequencia).containsExactly(10, 20);
            assertThat(passosCaptor.getValue()).extracting(RoteiroPasso::getEtapaSetor).containsExactly(null, etapa);
            assertThat(passosCaptor.getValue()).allMatch(p -> p.getRoteiro() == copiaCaptor.getValue());
        }
    }

    @Nested
    @DisplayName("excluir")
    class Excluir {

        @Test
        @DisplayName("Roteiro inexistente é rejeitado")
        void inexistente() {
            when(roteiroRepository.findById(1L)).thenReturn(Optional.empty());

            assertChave(() -> service.excluir(1L), AppMessages.ERROR_ROUTE_NOTFOUND);
        }

        @Test
        @DisplayName("Roteiro com produtos vinculados não pode ser excluído")
        void comProdutos() {
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(roteiro(1L, "R-1")));
            when(produtoRepository.countByRoteiroId(1L)).thenReturn(2L);

            assertChave(() -> service.excluir(1L), AppMessages.ERROR_ROUTE_HAS_PRODUCTS);

            verify(roteiroRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Roteiro já usado em ordens não pode ser excluído")
        void usadoEmOrdens() {
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(roteiro(1L, "R-1")));
            when(produtoRepository.countByRoteiroId(1L)).thenReturn(0L);
            when(ordemProducaoRepository.existsByRoteiroId(1L)).thenReturn(true);

            assertChave(() -> service.excluir(1L), AppMessages.ERROR_ROUTE_IN_USE);

            verify(roteiroRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Roteiro livre é excluído")
        void exclui() {
            Roteiro r = roteiro(1L, "R-1");
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(r));
            when(produtoRepository.countByRoteiroId(1L)).thenReturn(0L);
            when(ordemProducaoRepository.existsByRoteiroId(1L)).thenReturn(false);

            service.excluir(1L);

            verify(roteiroRepository).delete(r);
        }
    }

    @Nested
    @DisplayName("vincularProdutos")
    class Vincular {

        private final Roteiro roteiro = roteiro(1L, "R-1");

        @Test
        @DisplayName("Roteiro inexistente é rejeitado")
        void roteiroInexistente() {
            when(roteiroRepository.findById(1L)).thenReturn(Optional.empty());

            assertChave(() -> service.vincularProdutos(1L, List.of(1L), List.of()), AppMessages.ERROR_ROUTE_NOTFOUND);
        }

        @Test
        @DisplayName("Listas nulas ou vazias não consultam produtos")
        void semAlteracoes() {
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(roteiro));

            service.vincularProdutos(1L, null, null);
            service.vincularProdutos(1L, List.of(), List.of());

            verifyNoInteractions(produtoRepository);
        }

        @Test
        @DisplayName("Vincula os produtos a adicionar ao roteiro")
        void adiciona() {
            Produto a = produto(1L, "A", null);
            Produto b = produto(2L, "B", roteiro(9L, "OUTRO"));
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(roteiro));
            when(produtoRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(a, b));

            service.vincularProdutos(1L, List.of(1L, 2L), null);

            assertThat(a.getRoteiro()).isSameAs(roteiro);
            assertThat(b.getRoteiro()).isSameAs(roteiro);
        }

        @Test
        @DisplayName("Produto a adicionar inexistente rejeita toda a operação")
        void adicionaInexistente() {
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(roteiro));
            when(produtoRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(produto(1L, "A", null)));

            assertChave(() -> service.vincularProdutos(1L, List.of(1L, 2L), null), AppMessages.ERROR_PRODUCT_NOTFOUND);
        }

        @Test
        @DisplayName("Remove o vínculo somente de produtos vinculados a ESTE roteiro")
        void removeSomenteDoProprioRoteiro() {
            Produto deste = produto(1L, "A", roteiro);
            Produto deOutro = produto(2L, "B", roteiro(9L, "OUTRO"));
            Produto semRoteiro = produto(3L, "C", null);
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(roteiro));
            when(produtoRepository.findAllById(Set.of(1L, 2L, 3L))).thenReturn(List.of(deste, deOutro, semRoteiro));

            service.vincularProdutos(1L, null, List.of(1L, 2L, 3L));

            assertThat(deste.getRoteiro()).isNull();
            assertThat(deOutro.getRoteiro()).isNotNull();
            assertThat(deOutro.getRoteiro().getId()).isEqualTo(9L);
            assertThat(semRoteiro.getRoteiro()).isNull();
        }

        @Test
        @DisplayName("Ids presentes nas duas listas (contraditórios) são ignorados")
        void contraditoriosSaoIgnorados() {
            Produto novo = produto(1L, "A", null);
            Produto contraditorio = produto(2L, "B", roteiro);
            when(roteiroRepository.findById(1L)).thenReturn(Optional.of(roteiro));
            when(produtoRepository.findAllById(Set.of(1L))).thenReturn(List.of(novo));

            service.vincularProdutos(1L, List.of(1L, 2L), List.of(2L));

            assertThat(novo.getRoteiro()).isSameAs(roteiro);
            assertThat(contraditorio.getRoteiro()).isSameAs(roteiro);
            verify(produtoRepository, never()).findAllById(Set.of(2L));
        }
    }
}
