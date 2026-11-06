package br.com.lumiflow.controller;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.RoteiroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoteiroRestController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(RoteiroRestController.class)
@DisplayName("RoteiroRestController")
class RoteiroRestControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    RoteiroService roteiroService;

    // ---------- /produto/{produtoId} ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("retorna o fluxo do produto como lista JSON de textos")
    void fluxoDoProduto() throws Exception {
        when(roteiroService.buscarFluxoPorProdutoId(4L)).thenReturn(List.of("Corte", "Lapidação"));

        mockMvc.perform(get("/api/roteiros/produto/4"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Corte"))
                .andExpect(jsonPath("$[1]").value("Lapidação"));
    }

    @Test
    @WithMockUser
    @DisplayName("BusinessException vira 400")
    void fluxoBusinessExceptionVira400() throws Exception {
        when(roteiroService.buscarFluxoPorProdutoId(4L)).thenThrow(new BusinessException("error.product.without.route"));

        mockMvc.perform(get("/api/roteiros/produto/4"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("falha de acesso a dados vira 500")
    void fluxoDataAccessVira500() throws Exception {
        when(roteiroService.buscarFluxoPorProdutoId(4L)).thenThrow(new DataRetrievalFailureException("banco fora"));

        mockMvc.perform(get("/api/roteiros/produto/4"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    @DisplayName("erro inesperado vira 500")
    void fluxoErroInesperadoVira500() throws Exception {
        when(roteiroService.buscarFluxoPorProdutoId(4L)).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get("/api/roteiros/produto/4"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    @DisplayName("id de produto não numérico responde 400 (fluxo padrão do Spring)")
    void fluxoIdNaoNumerico() throws Exception {
        mockMvc.perform(get("/api/roteiros/produto/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(roteiroService);
    }

    // ---------- /{roteiroId}/setor/{setorId}/etapas ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("retorna as etapas do setor no roteiro em JSON")
    void etapasDoSetor() throws Exception {
        when(roteiroService.buscarEtapasDoSetorNoRoteiro(7L, 3L)).thenReturn(List.of(
                new EtapaSetorDTO(10L, "Corte reto", 1),
                new EtapaSetorDTO(11L, "Corte curvo", 2)));

        mockMvc.perform(get("/api/roteiros/7/setor/3/etapas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nome").value("Corte reto"))
                .andExpect(jsonPath("$[1].ordem").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("BusinessException em /etapas vira 404 ProblemDetail (sem redirect)")
    void etapasBusinessExceptionVira404() throws Exception {
        when(roteiroService.buscarEtapasDoSetorNoRoteiro(7L, 3L))
                .thenThrow(new BusinessException("error.route.notfound"));

        mockMvc.perform(get("/api/roteiros/7/setor/3/etapas"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("erro inesperado em /etapas vira 500 ProblemDetail sem vazar o detalhe")
    void etapasErroInesperadoVira500() throws Exception {
        when(roteiroService.buscarEtapasDoSetorNoRoteiro(7L, 3L)).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get("/api/roteiros/7/setor/3/etapas"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("falha"))));
    }

    // ---------- Autorização ----------

    @ParameterizedTest(name = "perfil {0} acessa a API")
    @ValueSource(strings = {"OPERADOR", "GESTAO", "PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("qualquer perfil autenticado acessa a API de roteiros")
    void qualquerAutenticado(String role) throws Exception {
        when(roteiroService.buscarFluxoPorProdutoId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/roteiros/produto/1").with(perfil(role)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("anônimo recebe 401 (e não redirect para o login)")
    void anonimo() throws Exception {
        mockMvc.perform(get("/api/roteiros/produto/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/roteiros/7/setor/3/etapas"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(roteiroService);
    }
}
