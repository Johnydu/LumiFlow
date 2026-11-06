package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.comum.PaginaDTO;
import br.com.lumiflow.dto.produto.ProdutoBuscaDTO;
import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.produto.ProdutoListagemDTO;
import br.com.lumiflow.entity.Produto;
import br.com.lumiflow.entity.Roteiro;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.ProdutoMapper;
import br.com.lumiflow.repository.ProdutoRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;
    @Mock
    private ProdutoMapper produtoMapper;

    @InjectMocks
    private ProdutoService service;

    private Produto produto(Long id, String nome, String codigo) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setCodigo(codigo);
        return p;
    }

    private static void assertChave(Runnable acao, String chave) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", chave);
    }

    @Nested
    @DisplayName("novoProduto")
    class Novo {

        @Test
        @DisplayName("DTO nulo é rejeitado")
        void dtoNulo() {
            assertChave(() -> service.novoProduto(null), AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(produtoRepository, produtoMapper);
        }

        @Test
        @DisplayName("Nome duplicado (normalizado) é rejeitado")
        void nomeDuplicado() {
            when(produtoRepository.existsByNome("PORTA")).thenReturn(true);

            assertChave(() -> service.novoProduto(new ProdutoDTO(null, " porta ", "P-01", null)),
                    AppMessages.ERROR_PRODUCT_NOME_DUPLICATE);

            verify(produtoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Código em branco é rejeitado como falha de validação")
        void codigoEmBranco() {
            assertChave(() -> service.novoProduto(new ProdutoDTO(null, null, "  ", null)),
                    AppMessages.ERROR_VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Código duplicado (normalizado) é rejeitado")
        void codigoDuplicado() {
            when(produtoRepository.existsByCodigo("P-01")).thenReturn(true);

            assertChave(() -> service.novoProduto(new ProdutoDTO(null, null, " p-01 ", null)),
                    AppMessages.ERROR_PRODUCT_CODIGO_DUPLICATE);

            verify(produtoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nome é opcional: sem nome não consulta duplicidade de nome")
        void nomeOpcional() {
            ProdutoDTO dto = new ProdutoDTO(null, null, "P-02", null);
            when(produtoMapper.toEntity(dto)).thenReturn(new Produto());

            service.novoProduto(dto);

            verify(produtoRepository, never()).existsByNome(any());
            ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
            verify(produtoRepository).save(captor.capture());
            assertThat(captor.getValue().getNome()).isNull();
            assertThat(captor.getValue().getCodigo()).isEqualTo("P-02");
        }

        @Test
        @DisplayName("Salva com nome normalizado e código sem espaços nas pontas")
        void salvaNormalizado() {
            ProdutoDTO dto = new ProdutoDTO(null, "  porta de vidro ", "  p-01 ", "desc");
            when(produtoMapper.toEntity(dto)).thenReturn(new Produto());

            service.novoProduto(dto);

            ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
            verify(produtoRepository).save(captor.capture());
            assertThat(captor.getValue().getNome()).isEqualTo("PORTA DE VIDRO");
            assertThat(captor.getValue().getCodigo()).isEqualTo("p-01");
        }

        @Test
        @DisplayName("Erros de banco e inesperados viram falha de validação")
        void erros() {
            ProdutoDTO dto = new ProdutoDTO(null, null, "P-03", null);
            when(produtoMapper.toEntity(dto)).thenReturn(new Produto());
            when(produtoRepository.save(any(Produto.class)))
                    .thenThrow(new DataRetrievalFailureException("x"))
                    .thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.novoProduto(dto), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.novoProduto(dto), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("buscarProdutoPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Retorna o produto encontrado")
        void encontrado() {
            Produto p = produto(1L, "A", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));

            assertThat(service.buscarProdutoPorId(1L)).isSameAs(p);
        }

        @Test
        @DisplayName("Id nulo, inexistente ou falha de banco resultam em produto não encontrado")
        void naoEncontrado() {
            when(produtoRepository.findById(2L)).thenReturn(Optional.empty());
            when(produtoRepository.findById(3L)).thenThrow(new DataRetrievalFailureException("x"));
            when(produtoRepository.findById(4L)).thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.buscarProdutoPorId(null), AppMessages.ERROR_PRODUCT_NOTFOUND);
            assertChave(() -> service.buscarProdutoPorId(2L), AppMessages.ERROR_PRODUCT_NOTFOUND);
            assertChave(() -> service.buscarProdutoPorId(3L), AppMessages.ERROR_PRODUCT_NOTFOUND);
            assertChave(() -> service.buscarProdutoPorId(4L), AppMessages.ERROR_PRODUCT_NOTFOUND);
        }
    }

    @Nested
    @DisplayName("deletarProduto")
    class Deletar {

        @Test
        @DisplayName("Exclui e faz flush (nessa ordem)")
        void exclui() {
            Produto p = produto(1L, "A", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));

            service.deletarProduto(1L);

            InOrder ordem = Mockito.inOrder(produtoRepository);
            ordem.verify(produtoRepository).delete(p);
            ordem.verify(produtoRepository).flush();
        }

        @Test
        @DisplayName("Produto inexistente propaga não encontrado")
        void inexistente() {
            when(produtoRepository.findById(1L)).thenReturn(Optional.empty());

            assertChave(() -> service.deletarProduto(1L), AppMessages.ERROR_PRODUCT_NOTFOUND);

            verify(produtoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Produto usado em ordens (violação de integridade) vira registro em uso")
        void emUso() {
            Produto p = produto(1L, "A", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));
            Mockito.doThrow(new DataIntegrityViolationException("fk")).when(produtoRepository).flush();

            assertChave(() -> service.deletarProduto(1L), AppMessages.ERROR_RECORD_IN_USE);
        }

        @Test
        @DisplayName("Outros erros de banco viram falha de validação")
        void erroDeBanco() {
            Produto p = produto(1L, "A", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));
            Mockito.doThrow(new DataRetrievalFailureException("x")).when(produtoRepository).delete(p);

            assertChave(() -> service.deletarProduto(1L), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("editarProduto")
    class Editar {

        @Test
        @DisplayName("DTO nulo é rejeitado")
        void dtoNulo() {
            assertChave(() -> service.editarProduto(1L, null), AppMessages.ERROR_VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Produto inexistente é rejeitado")
        void inexistente() {
            when(produtoRepository.findById(1L)).thenReturn(Optional.empty());

            assertChave(() -> service.editarProduto(1L, new ProdutoDTO(1L, "A", "A1", null)),
                    AppMessages.ERROR_PRODUCT_NOTFOUND);
        }

        @Test
        @DisplayName("Apagar o nome no formulário limpa o campo e atualiza a descrição")
        void nomeEmBrancoLimpaCampo() {
            Produto p = produto(1L, "ANTIGO", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));

            service.editarProduto(1L, new ProdutoDTO(1L, "  ", "A1", "nova descrição"));

            assertThat(p.getNome()).isNull();
            assertThat(p.getDescricao()).isEqualTo("nova descrição");
            verify(produtoRepository).save(p);
        }

        @Test
        @DisplayName("Mudar para um nome já existente é rejeitado")
        void nomeDuplicado() {
            Produto p = produto(1L, "ANTIGO", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));
            when(produtoRepository.existsByNome("PORTA")).thenReturn(true);

            assertChave(() -> service.editarProduto(1L, new ProdutoDTO(1L, "porta", "A1", null)),
                    AppMessages.ERROR_PRODUCT_NOME_DUPLICATE);

            verify(produtoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Manter o mesmo nome (ignorando caixa) não consulta duplicidade")
        void mesmoNomeNaoConsultaDuplicidade() {
            Produto p = produto(1L, "PORTA", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));

            service.editarProduto(1L, new ProdutoDTO(1L, "porta", "A1", null));

            verify(produtoRepository, never()).existsByNome(any());
            assertThat(p.getNome()).isEqualTo("PORTA");
        }

        @Test
        @DisplayName("Mudar para um código já existente é rejeitado")
        void codigoDuplicado() {
            Produto p = produto(1L, "PORTA", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));
            when(produtoRepository.existsByCodigo("B2")).thenReturn(true);

            assertChave(() -> service.editarProduto(1L, new ProdutoDTO(1L, "PORTA", "b2", null)),
                    AppMessages.ERROR_PRODUCT_CODIGO_DUPLICATE);

            verify(produtoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Atualiza código normalizado quando é inédito")
        void atualizaCodigo() {
            Produto p = produto(1L, "PORTA", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));
            when(produtoRepository.existsByCodigo("C3")).thenReturn(false);

            service.editarProduto(1L, new ProdutoDTO(1L, "PORTA", " c3 ", null));

            assertThat(p.getCodigo()).isEqualTo("C3");
            verify(produtoRepository).save(p);
        }

        @Test
        @DisplayName("Código em branco no DTO mantém o código atual")
        void codigoEmBrancoMantemAtual() {
            Produto p = produto(1L, "PORTA", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));

            service.editarProduto(1L, new ProdutoDTO(1L, "PORTA", "  ", null));

            assertThat(p.getCodigo()).isEqualTo("A1");
            verify(produtoRepository, never()).existsByCodigo(any());
        }

        @Test
        @DisplayName("Erro de banco ao salvar vira falha de validação")
        void erroDeBanco() {
            Produto p = produto(1L, "PORTA", "A1");
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(p));
            when(produtoRepository.save(p)).thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.editarProduto(1L, new ProdutoDTO(1L, "PORTA", "A1", null)),
                    AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("buscas paginadas")
    class Buscas {

        @Test
        @DisplayName("buscarProdutos usa páginas de 25, trim no termo e preserva o total")
        void buscarProdutos() {
            List<Produto> conteudo = List.of(produto(1L, "A", "A1"));
            List<ProdutoListagemDTO> dtos = List.of(new ProdutoListagemDTO(1L, "A", "A1", null, null, null));
            when(produtoRepository.buscarPorTermo("porta", PageRequest.of(0, 25)))
                    .thenReturn(new PageImpl<>(conteudo, PageRequest.of(0, 25), 60));
            when(produtoMapper.toListagemDTO(conteudo)).thenReturn(dtos);

            Page<ProdutoListagemDTO> pagina = service.buscarProdutos("  porta ", 0);

            assertThat(pagina.getContent()).isEqualTo(dtos);
            assertThat(pagina.getTotalElements()).isEqualTo(60);
            assertThat(pagina.getSize()).isEqualTo(25);
        }

        @Test
        @DisplayName("buscarProdutos trata termo nulo como vazio e página negativa como zero")
        void buscarProdutosLimites() {
            when(produtoRepository.buscarPorTermo("", PageRequest.of(0, 25))).thenReturn(Page.empty());
            when(produtoMapper.toListagemDTO(List.<Produto>of())).thenReturn(List.of());

            assertThat(service.buscarProdutos(null, -5).getContent()).isEmpty();

            verify(produtoRepository).buscarPorTermo("", PageRequest.of(0, 25));
        }

        @Test
        @DisplayName("buscarProdutos converte erro de banco em falha de validação")
        void buscarProdutosErro() {
            when(produtoRepository.buscarPorTermo("x", PageRequest.of(0, 25)))
                    .thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.buscarProdutos("x", 0), AppMessages.ERROR_VALIDATION_FAILED);
        }

        @Test
        @DisplayName("buscarParaSelecao usa páginas de 20 e informa se há mais resultados")
        void buscarParaSelecao() {
            Roteiro roteiro = new Roteiro();
            roteiro.setCodigo("R-1");
            Produto comRoteiro = produto(1L, "A", "A1");
            comRoteiro.setRoteiro(roteiro);
            Produto semRoteiro = produto(2L, "B", "B1");
            when(produtoRepository.buscarPorTermo("", PageRequest.of(1, 20)))
                    .thenReturn(new PageImpl<>(List.of(comRoteiro, semRoteiro), PageRequest.of(1, 20), 45));

            PaginaDTO<ProdutoBuscaDTO> pagina = service.buscarParaSelecao(null, 1);

            assertThat(pagina.itens()).containsExactly(
                    new ProdutoBuscaDTO(1L, "A", "A1", "R-1"),
                    new ProdutoBuscaDTO(2L, "B", "B1", null));
            assertThat(pagina.pagina()).isEqualTo(1);
            assertThat(pagina.temMais()).isTrue();
            assertThat(pagina.total()).isEqualTo(45);
        }

        @Test
        @DisplayName("buscarParaSelecao na última página informa que não há mais resultados")
        void ultimaPagina() {
            when(produtoRepository.buscarPorTermo("a", PageRequest.of(0, 20)))
                    .thenReturn(new PageImpl<>(List.of(produto(1L, "A", "A1")), PageRequest.of(0, 20), 1));

            PaginaDTO<ProdutoBuscaDTO> pagina = service.buscarParaSelecao(" a ", -1);

            assertThat(pagina.temMais()).isFalse();
            assertThat(pagina.pagina()).isZero();
        }

        @Test
        @DisplayName("buscarParaSelecaoPorId retorna nulo para id nulo ou inexistente e o DTO quando existe")
        void buscarParaSelecaoPorId() {
            when(produtoRepository.findById(2L)).thenReturn(Optional.empty());
            when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto(1L, "A", "A1")));

            assertThat(service.buscarParaSelecaoPorId(null)).isNull();
            assertThat(service.buscarParaSelecaoPorId(2L)).isNull();
            assertThat(service.buscarParaSelecaoPorId(1L)).isEqualTo(new ProdutoBuscaDTO(1L, "A", "A1", null));
        }
    }
}
