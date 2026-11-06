package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.OrdemDetalheDTO;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OrdemDetalheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = OrdemDetalheController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(OrdemDetalheController.class)
@DisplayName("OrdemDetalheController")
class OrdemDetalheControllerTest {

    private static final String URL = "/dashboard/ordens/10/detalhes";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    OrdemDetalheService ordemDetalheService;

    private OrdemDetalheDTO detalhe() {
        return new OrdemDetalheDTO(10L, "OP-10", "Vidro", "P1", 100, LocalDateTime.of(2026, 1, 15, 8, 0),
                "obs", EstatusOrdemProducao.EM_ANDAMENTO, 40, 2, 40.0, List.of(), List.of(), List.of());
    }

    @ParameterizedTest(name = "perfil {0} vê os detalhes")
    @ValueSource(strings = {"GESTAO", "PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("perfis gerenciais recebem a view com o detalhe da ordem")
    void perfisPermitidos(String role) throws Exception {
        OrdemDetalheDTO dto = detalhe();
        when(ordemDetalheService.buscarDetalhe(10L)).thenReturn(dto);

        mockMvc.perform(get(URL).with(perfil(role)))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/DetalhesOrdem"))
                .andExpect(model().attribute("ordem", dto));
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"OPERADOR", "USER"})
    @DisplayName("OPERADOR e perfis desconhecidos recebem 403 e o service não é chamado")
    void perfisProibidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ordemDetalheService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(ordemDetalheService);
    }

    @Test
    @DisplayName("ordem inexistente (BusinessException) volta ao Referer com flash de erro")
    void ordemInexistenteComReferer() throws Exception {
        when(ordemDetalheService.buscarDetalhe(10L)).thenThrow(new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND));

        mockMvc.perform(get(URL).with(perfil("PCP")).header("Referer", "http://localhost/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/dashboard"))
                .andExpect(flash().attribute("error",
                        messageSource.getMessage(AppMessages.ERROR_ORDER_NOTFOUND, null, PT_BR)));
    }

    @Test
    @DisplayName("ordem inexistente sem Referer vai para a lista de ordens por setor")
    void ordemInexistenteSemReferer() throws Exception {
        when(ordemDetalheService.buscarDetalhe(10L)).thenThrow(new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND));

        mockMvc.perform(get(URL).with(perfil("PCP")))
                .andExpect(redirectedUrl("/dashboard/ordens-setor"));
    }
}
