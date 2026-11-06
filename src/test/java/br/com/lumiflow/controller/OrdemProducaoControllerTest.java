package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.OrdemProducaoDTO;
import br.com.lumiflow.dto.produto.ProdutoBuscaDTO;
import br.com.lumiflow.entity.enums.Descricao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OrdemProducaoService;
import br.com.lumiflow.service.ProdutoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static br.com.lumiflow.controller.ControllerTestSupport.logadoComo;
import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

@WebMvcTest(controllers = OrdemProducaoController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(OrdemProducaoController.class)
@DisplayName("OrdemProducaoController")
class OrdemProducaoControllerTest {

    private static final String URL = "/dashboard/ordens";
    private static final String NOVA_ORDEM = URL + "/novaOrdem";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    OrdemProducaoService ordemProducaoService;
    @MockitoBean
    ProdutoService produtoService;

    private String msg(String chave) {
        return messageSource.getMessage(chave, null, PT_BR);
    }

    private MockHttpServletRequestBuilder postValido() {
        return post(URL).with(csrf())
                .param("produtoId", "1").param("quantidade", "10")
                .param("dataCriacao", "2026-01-15").param("observacao", "obs");
    }

    // ---------- GET novaOrdem ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET novaOrdem entrega DTO vazio e produto selecionado nulo")
    void formularioVazio() throws Exception {
        mockMvc.perform(get(NOVA_ORDEM))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/NovaOrdem"))
                .andExpect(model().attribute("ordemDTO", new OrdemProducaoDTO(null, null, null, null)))
                .andExpect(model().attribute("produtoSelecionado", nullValue()));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET novaOrdem com DTO vindo do flash recarrega o produto selecionado")
    void formularioComProdutoSelecionado() throws Exception {
        OrdemProducaoDTO dto = new OrdemProducaoDTO(3L, 5, LocalDate.of(2026, 1, 15), null);
        ProdutoBuscaDTO produto = new ProdutoBuscaDTO(3L, "Vidro", "P1", "R1");
        when(produtoService.buscarParaSelecaoPorId(3L)).thenReturn(produto);

        mockMvc.perform(get(NOVA_ORDEM).flashAttr("ordemDTO", dto))
                .andExpect(status().isOk())
                .andExpect(model().attribute("ordemDTO", dto))
                .andExpect(model().attribute("produtoSelecionado", produto));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET novaOrdem com falha no service mostra mensagem de erro e mantém o formulário")
    void formularioComFalha() throws Exception {
        OrdemProducaoDTO dto = new OrdemProducaoDTO(3L, 5, LocalDate.of(2026, 1, 15), null);
        when(produtoService.buscarParaSelecaoPorId(anyLong())).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(NOVA_ORDEM).flashAttr("ordemDTO", dto))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/NovaOrdem"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(model().attribute("ordemDTO", dto));
    }

    // ---------- POST criar ----------

    @Test
    @DisplayName("POST válido cria a ordem com o id do usuário logado e redireciona ao dashboard")
    void criaOrdem() throws Exception {
        mockMvc.perform(postValido().with(logadoComo(7L, "Paula", Descricao.PCP)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_ORDER_CREATED)));

        verify(ordemProducaoService).criar(new OrdemProducaoDTO(1L, 10, LocalDate.of(2026, 1, 15), "obs"), 7L);
    }

    @Test
    @DisplayName("POST com quantidade zero reexibe o formulário com a mensagem de quantidade positiva")
    void criaQuantidadeZero() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).with(logadoComo(7L, "Paula", Descricao.PCP))
                        .param("produtoId", "1").param("quantidade", "0").param("dataCriacao", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/NovaOrdem"))
                .andExpect(model().attribute("error", msg("validation.quantity.positive")))
                .andExpect(model().attributeHasFieldErrors("ordemDTO", "quantidade"));

        verify(ordemProducaoService, never()).criar(any(), any());
    }

    @Test
    @DisplayName("POST sem produto informa que o produto é obrigatório")
    void criaSemProduto() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).with(logadoComo(7L, "Paula", Descricao.PCP))
                        .param("quantidade", "5").param("dataCriacao", "2026-01-15"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", msg("validation.product.required")));
    }

    @Test
    @DisplayName("POST sem data informa que a data é obrigatória")
    void criaSemData() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).with(logadoComo(7L, "Paula", Descricao.PCP))
                        .param("produtoId", "1").param("quantidade", "5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", msg("validation.date.required")));
    }

    @Test
    @DisplayName("POST com observação acima de 500 caracteres é rejeitado")
    void criaObservacaoGrande() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).with(logadoComo(7L, "Paula", Descricao.PCP))
                        .param("produtoId", "1").param("quantidade", "5").param("dataCriacao", "2026-01-15")
                        .param("observacao", "x".repeat(501)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", msg("validation.notes.size500")));
    }

    @Test
    @DisplayName("POST com BusinessException devolve flash de erro e o DTO para o formulário")
    void criaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE))
                .when(ordemProducaoService).criar(any(OrdemProducaoDTO.class), anyLong());

        mockMvc.perform(postValido().with(logadoComo(7L, "Paula", Descricao.PCP)))
                .andExpect(redirectedUrl(NOVA_ORDEM))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_PRODUCT_WITHOUT_ROUTE)))
                .andExpect(flash().attribute("ordemDTO",
                        new OrdemProducaoDTO(1L, 10, LocalDate.of(2026, 1, 15), "obs")));
    }

    @Test
    @DisplayName("POST com erro inesperado devolve mensagem genérica e o DTO")
    void criaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha"))
                .when(ordemProducaoService).criar(any(OrdemProducaoDTO.class), anyLong());

        mockMvc.perform(postValido().with(logadoComo(7L, "Paula", Descricao.PCP)))
                .andExpect(redirectedUrl(NOVA_ORDEM))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(flash().attributeExists("ordemDTO"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com principal que não é UsuarioDetails é negado (403) e não cria a ordem")
    void criaComPrincipalSemId() throws Exception {
        mockMvc.perform(postValido())
                .andExpect(status().isForbidden());

        verifyNoInteractions(ordemProducaoService);
    }

    @Test
    @DisplayName("POST sem CSRF é rejeitado com 403")
    void criaSemCsrf() throws Exception {
        mockMvc.perform(post(URL).with(logadoComo(7L, "Paula", Descricao.PCP))
                        .param("produtoId", "1").param("quantidade", "10").param("dataCriacao", "2026-01-15"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ordemProducaoService);
    }

    // ---------- Autorização ----------

    @ParameterizedTest(name = "perfil {0} acessa a criação de ordens")
    @ValueSource(strings = {"PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("PCP e superiores acessam o formulário")
    void perfisPermitidos(String role) throws Exception {
        mockMvc.perform(get(NOVA_ORDEM).with(perfil(role)))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/NovaOrdem"));
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"GESTAO", "OPERADOR"})
    @DisplayName("GESTAO e OPERADOR não criam ordens")
    void perfisProibidos(String role) throws Exception {
        mockMvc.perform(get(NOVA_ORDEM).with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(postValido().with(perfil(role))).andExpect(status().isForbidden());

        verifyNoInteractions(ordemProducaoService, produtoService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(NOVA_ORDEM))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(postValido())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(ordemProducaoService);
    }
}
