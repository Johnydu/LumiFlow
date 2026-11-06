package br.com.lumiflow.controller;

import br.com.lumiflow.dto.comum.PaginaDTO;
import br.com.lumiflow.dto.produto.ProdutoBuscaDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.ProdutoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProdutoRestController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(ProdutoRestController.class)
@DisplayName("ProdutoRestController")
class ProdutoRestControllerTest {

    private static final String URL = "/api/produtos/busca";

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    ProdutoService produtoService;

    @ParameterizedTest(name = "perfil {0} busca produtos")
    @ValueSource(strings = {"PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("PCP e superiores recebem a página de produtos em JSON")
    void retornaJson(String role) throws Exception {
        when(produtoService.buscarParaSelecao("vid", 1)).thenReturn(new PaginaDTO<>(
                List.of(new ProdutoBuscaDTO(1L, "Vidro", "P1", "R1")), 1, true, 25L));

        mockMvc.perform(get(URL).param("busca", "vid").param("pagina", "1").with(perfil(role)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.itens.length()").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(1))
                .andExpect(jsonPath("$.itens[0].nome").value("Vidro"))
                .andExpect(jsonPath("$.itens[0].codigo").value("P1"))
                .andExpect(jsonPath("$.itens[0].roteiroCodigo").value("R1"))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.temMais").value(true))
                .andExpect(jsonPath("$.total").value(25));
    }

    @Test
    @DisplayName("sem parâmetros usa busca vazia e página 0")
    void valoresPadrao() throws Exception {
        when(produtoService.buscarParaSelecao("", 0)).thenReturn(new PaginaDTO<>(List.of(), 0, false, 0L));

        mockMvc.perform(get(URL).with(perfil("PCP")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(0))
                .andExpect(jsonPath("$.temMais").value(false));

        verify(produtoService).buscarParaSelecao("", 0);
    }

    @Test
    @DisplayName("página não numérica responde 400 (tipo de parâmetro inválido), sem redirect")
    void paginaNaoNumerica() throws Exception {
        mockMvc.perform(get(URL).param("pagina", "abc").with(perfil("PCP")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(produtoService);
    }

    @Test
    @DisplayName("BusinessException do service vira ProblemDetail com o status da chave (404 para notfound)")
    void businessExceptionViraProblemDetail() throws Exception {
        when(produtoService.buscarParaSelecao("x", 0)).thenThrow(new BusinessException("error.produto.notfound"));

        mockMvc.perform(get(URL).param("busca", "x").with(perfil("PCP")))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"GESTAO", "OPERADOR"})
    @DisplayName("perfis sem permissão recebem 403 (método protegido por @PreAuthorize)")
    void perfisProibidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(produtoService);
    }

    @Test
    @DisplayName("anônimo em /api/** recebe 401 (e não redirect para o login)")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(produtoService);
    }
}
