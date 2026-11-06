package br.com.lumiflow.controller;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.EtapaSetorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EtapaSetorController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(EtapaSetorController.class)
@DisplayName("EtapaSetorController")
class EtapaSetorControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    EtapaSetorService etapaSetorService;

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("retorna as etapas do setor em JSON")
    void retornaEtapasEmJson() throws Exception {
        when(etapaSetorService.listarPorSetorId(3L)).thenReturn(List.of(
                new EtapaSetorDTO(1L, "Corte", 1),
                new EtapaSetorDTO(2L, "Lapidação", 2)));

        mockMvc.perform(get("/dashboard/setores/3/etapas"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Corte"))
                .andExpect(jsonPath("$[1].ordem").value(2));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("setor sem etapas retorna lista vazia")
    void setorSemEtapas() throws Exception {
        when(etapaSetorService.listarPorSetorId(3L)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/setores/3/etapas"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("BusinessException vira 404")
    void businessExceptionVira404() throws Exception {
        when(etapaSetorService.listarPorSetorId(9L)).thenThrow(new BusinessException("error.sector.notfound"));

        mockMvc.perform(get("/dashboard/setores/9/etapas"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("erro inesperado vira 500")
    void erroInesperadoVira500() throws Exception {
        when(etapaSetorService.listarPorSetorId(9L)).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get("/dashboard/setores/9/etapas"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("id não numérico responde 400 (fluxo padrão do Spring), não redirect 302")
    void idNaoNumerico() throws Exception {
        mockMvc.perform(get("/dashboard/setores/abc/etapas"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(etapaSetorService);
    }

    @ParameterizedTest(name = "perfil {0} é permitido")
    @ValueSource(strings = {"PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("perfis PCP e superiores acessam")
    void perfisPermitidos(String role) throws Exception {
        when(etapaSetorService.listarPorSetorId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard/setores/1/etapas").with(perfil(role)))
                .andExpect(status().isOk());
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"GESTAO", "OPERADOR"})
    @DisplayName("perfis sem permissão de cadastro recebem 403")
    void perfisProibidos(String role) throws Exception {
        mockMvc.perform(get("/dashboard/setores/1/etapas").with(perfil(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(etapaSetorService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get("/dashboard/setores/1/etapas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
