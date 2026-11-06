package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.dashboard.DashboardOrdemDTO;
import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.service.DashboardService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = DashboardController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(DashboardController.class)
@DisplayName("DashboardController")
class DashboardControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    DashboardService dashboardService;

    private DashboardOrdemDTO ordem() {
        return new DashboardOrdemDTO(1L, "OP-1", "Produto", "P1", 10, 50.0, 1, 2,
                "Corte", "Setor A", EstatusOrdemProducao.EM_ANDAMENTO);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("carrega métricas, ordens e resumo de setores no model")
    void carregaMetricas() throws Exception {
        DashboardOrdemDTO ordem = ordem();
        SetorResumoDTO setor = new SetorResumoDTO(1L, "Corte", 3);
        when(dashboardService.obterResumoEstatisticas()).thenReturn(new DashboardResumoDTO(10L, 3L, 5L, 2L));
        when(dashboardService.listarOrdens(any(), any(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(ordem), PageRequest.of(0, 10), 1));
        when(dashboardService.obterResumoSetores()).thenReturn(List.of(setor));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/Dashboard"))
                .andExpect(model().attribute("totalOrdens", 10L))
                .andExpect(model().attribute("ordensAndamento", 3L))
                .andExpect(model().attribute("ordensConcluidas", 5L))
                .andExpect(model().attribute("ordensPendentes", 2L))
                .andExpect(model().attribute("ordens", List.of(ordem)))
                .andExpect(model().attribute("resumoSetores", List.of(setor)))
                .andExpect(model().attributeExists("paginaOrdens"))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("repassa busca, status e página ao service")
    void repassaFiltros() throws Exception {
        when(dashboardService.obterResumoEstatisticas()).thenReturn(new DashboardResumoDTO(0L, 0L, 0L, 0L));
        when(dashboardService.listarOrdens("vidro", "EM_ANDAMENTO", 2)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/dashboard").param("busca", "vidro").param("status", "EM_ANDAMENTO").param("page", "2"))
                .andExpect(status().isOk());

        verify(dashboardService).listarOrdens("vidro", "EM_ANDAMENTO", 2);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("sem filtros usa busca/status nulos e página 0")
    void semFiltros() throws Exception {
        when(dashboardService.listarOrdens(null, null, 0)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/dashboard")).andExpect(status().isOk());

        verify(dashboardService).listarOrdens(null, null, 0);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("resumo e setores nulos viram zeros e lista vazia")
    void resumoNulo() throws Exception {
        when(dashboardService.obterResumoEstatisticas()).thenReturn(null);
        when(dashboardService.listarOrdens(any(), any(), anyInt())).thenReturn(new PageImpl<>(List.of()));
        when(dashboardService.obterResumoSetores()).thenReturn(null);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalOrdens", 0L))
                .andExpect(model().attribute("ordensAndamento", 0L))
                .andExpect(model().attribute("ordensConcluidas", 0L))
                .andExpect(model().attribute("ordensPendentes", 0L))
                .andExpect(model().attribute("resumoSetores", List.of()));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("falha no service mantém a página com mensagem de erro e valores zerados")
    void falhaNoService() throws Exception {
        when(dashboardService.obterResumoEstatisticas()).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/Dashboard"))
                .andExpect(model().attribute("error",
                        messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, PT_BR)))
                .andExpect(model().attribute("totalOrdens", 0L))
                .andExpect(model().attribute("ordens", List.of()));
    }

    @ParameterizedTest(name = "perfil {0} acessa o dashboard")
    @ValueSource(strings = {"GESTAO", "PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("perfis gerenciais acessam o dashboard")
    void perfisPermitidos(String role) throws Exception {
        when(dashboardService.listarOrdens(any(), any(), anyInt())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/dashboard").with(perfil(role)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/Dashboard"));
    }

    @Test
    @DisplayName("OPERADOR recebe 403 e o service não é chamado")
    void operadorProibido() throws Exception {
        mockMvc.perform(get("/dashboard").with(perfil("OPERADOR")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(dashboardService);
    }
}
