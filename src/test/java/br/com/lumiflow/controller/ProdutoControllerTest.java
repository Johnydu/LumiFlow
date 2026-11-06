package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.produto.ProdutoListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.ProdutoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = ProdutoController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(ProdutoController.class)
@DisplayName("ProdutoController")
class ProdutoControllerTest {

    private static final String URL = "/dashboard/produtos";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    ProdutoService produtoService;

    private String msg(String chave, Object... args) {
        return messageSource.getMessage(chave, args, PT_BR);
    }

    // ---------- GET ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET lista produtos paginados, repassa nome e página e entrega DTO vazio")
    void listaProdutos() throws Exception {
        ProdutoListagemDTO produto = new ProdutoListagemDTO(1L, "Vidro", "P1", "desc", "R1", "Roteiro");
        when(produtoService.buscarProdutos("vid", 2))
                .thenReturn(new PageImpl<>(List.of(produto), PageRequest.of(2, 10), 21));

        mockMvc.perform(get(URL).param("nome", "vid").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("produto/Produtos"))
                .andExpect(model().attribute("produtos", List.of(produto)))
                .andExpect(model().attributeExists("pagina"))
                .andExpect(model().attribute("nomeAtual", "vid"))
                .andExpect(model().attribute("produtoDTO", new ProdutoDTO(null, null, null, null)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET sem parâmetros busca com nome nulo e página 0")
    void listaSemParametros() throws Exception {
        when(produtoService.buscarProdutos(null, 0)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(URL)).andExpect(status().isOk());

        verify(produtoService).buscarProdutos(null, 0);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET com falha no service mostra o erro (aviso ao usuário) e lista vazia")
    void getComFalha() throws Exception {
        when(produtoService.buscarProdutos(any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("produto/Produtos"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(model().attribute("produtos", List.of()));
    }

    // ---------- POST criar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST válido cria o produto e avisa sucesso com o nome")
    void criaProduto() throws Exception {
        mockMvc.perform(post(URL).with(csrf())
                        .param("nome", "Vidro").param("codigo", "P1").param("descricao", "desc"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_PRODUCT_CREATED, "Vidro")));

        verify(produtoService).novoProduto(new ProdutoDTO(null, "Vidro", "P1", "desc"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST sem nome identifica o produto pelo código na mensagem de sucesso")
    void criaSemNomeUsaCodigo() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("codigo", "P1"))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_PRODUCT_CREATED, "P1")));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST sem código reexibe a página com mensagem de código obrigatório")
    void criaSemCodigo() throws Exception {
        when(produtoService.buscarProdutos(null, 0)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(post(URL).with(csrf()).param("nome", "Vidro").param("codigo", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("produto/Produtos"))
                .andExpect(model().attribute("error", msg("validation.code.required")))
                .andExpect(model().attributeHasFieldErrors("produtoDTO", "codigo"));

        verify(produtoService, never()).novoProduto(any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com código acima de 30 caracteres é rejeitado")
    void criaCodigoGrande() throws Exception {
        when(produtoService.buscarProdutos(null, 0)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(post(URL).with(csrf()).param("codigo", "x".repeat(31)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", msg("validation.code.size30")));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com BusinessException devolve flash de erro e o DTO")
    void criaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_PRODUCT_CODIGO_DUPLICATE))
                .when(produtoService).novoProduto(any(ProdutoDTO.class));

        mockMvc.perform(post(URL).with(csrf()).param("codigo", "P1"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_PRODUCT_CODIGO_DUPLICATE)))
                .andExpect(flash().attribute("produtoDTO", new ProdutoDTO(null, null, "P1", null)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com erro inesperado devolve mensagem genérica")
    void criaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(produtoService).novoProduto(any(ProdutoDTO.class));

        mockMvc.perform(post(URL).with(csrf()).param("codigo", "P1"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- POST excluir ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir remove o produto e avisa sucesso")
    void excluiProduto() throws Exception {
        mockMvc.perform(post(URL + "/3/excluir").with(csrf()))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_PRODUCT_DELETED)));

        verify(produtoService).deletarProduto(3L);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir com BusinessException devolve flash de erro")
    void excluiComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND)).when(produtoService).deletarProduto(3L);

        mockMvc.perform(post(URL + "/3/excluir").with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_PRODUCT_NOTFOUND)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir com erro inesperado devolve mensagem genérica")
    void excluiComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(produtoService).deletarProduto(3L);

        mockMvc.perform(post(URL + "/3/excluir").with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- POST editar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar válido atualiza e avisa sucesso")
    void editaProduto() throws Exception {
        mockMvc.perform(post(URL + "/3/editar").with(csrf()).param("nome", "Vidro 2").param("codigo", "P1"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_PRODUCT_UPDATED, "Vidro 2")));

        verify(produtoService).editarProduto(eq(3L), eq(new ProdutoDTO(3L, "Vidro 2", "P1", null)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar inválido reexibe a página com erro")
    void editaInvalido() throws Exception {
        when(produtoService.buscarProdutos(null, 0)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(post(URL + "/3/editar").with(csrf()).param("codigo", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("produto/Produtos"))
                .andExpect(model().attribute("error", msg("validation.code.required")));

        verify(produtoService, never()).editarProduto(any(), any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar com BusinessException devolve flash de erro e o DTO")
    void editaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_PRODUCT_NOME_DUPLICATE))
                .when(produtoService).editarProduto(eq(3L), any(ProdutoDTO.class));

        mockMvc.perform(post(URL + "/3/editar").with(csrf()).param("codigo", "P1"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_PRODUCT_NOME_DUPLICATE)))
                .andExpect(flash().attributeExists("produtoDTO"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar com erro inesperado devolve mensagem genérica")
    void editaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(produtoService).editarProduto(eq(3L), any(ProdutoDTO.class));

        mockMvc.perform(post(URL + "/3/editar").with(csrf()).param("codigo", "P1"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- CSRF e autorização ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POSTs sem CSRF são rejeitados com 403")
    void postsSemCsrf() throws Exception {
        mockMvc.perform(post(URL).param("codigo", "P1")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/3/excluir")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/3/editar").param("codigo", "P1")).andExpect(status().isForbidden());

        verifyNoInteractions(produtoService);
    }

    @ParameterizedTest(name = "perfil {0} acessa produtos")
    @ValueSource(strings = {"PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("PCP e superiores têm acesso")
    void perfisPermitidos(String role) throws Exception {
        when(produtoService.buscarProdutos(null, 0)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isOk());
        mockMvc.perform(post(URL + "/3/excluir").with(perfil(role)).with(csrf()))
                .andExpect(redirectedUrl(URL));
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"GESTAO", "OPERADOR"})
    @DisplayName("GESTAO e OPERADOR não acessam produtos")
    void perfisProibidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).with(perfil(role)).with(csrf()).param("codigo", "P1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/3/excluir").with(perfil(role)).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(produtoService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
