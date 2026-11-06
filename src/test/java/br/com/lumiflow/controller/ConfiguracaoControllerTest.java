package br.com.lumiflow.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = ConfiguracaoController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(ConfiguracaoController.class)
@DisplayName("ConfiguracaoController")
class ConfiguracaoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @ParameterizedTest(name = "perfil {0} acessa as configurações")
    @ValueSource(strings = {"ADMIN", "SUPERVISOR", "GESTAO", "PCP"})
    @DisplayName("perfis permitidos recebem a view de configurações")
    void perfisPermitidos(String role) throws Exception {
        mockMvc.perform(get("/dashboard/configuracoes").with(perfil(role)))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracao/Configuracoes"));
    }

    @Test
    @DisplayName("OPERADOR recebe 403")
    void operadorProibido() throws Exception {
        mockMvc.perform(get("/dashboard/configuracoes").with(perfil("OPERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimoRedirecionado() throws Exception {
        mockMvc.perform(get("/dashboard/configuracoes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("POST sem CSRF é rejeitado com 403")
    void postSemCsrf() throws Exception {
        mockMvc.perform(post("/dashboard/configuracoes").with(perfil("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST com CSRF não tem handler e responde 405 (fluxo padrão do Spring)")
    void postComCsrfNaoSuportado() throws Exception {
        mockMvc.perform(post("/dashboard/configuracoes").with(perfil("ADMIN")).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }
}
