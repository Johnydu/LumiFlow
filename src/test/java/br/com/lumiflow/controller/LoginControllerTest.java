package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.entity.enums.Descricao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static br.com.lumiflow.controller.ControllerTestSupport.logadoComo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = LoginController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(LoginController.class)
@DisplayName("LoginController")
class LoginControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;

    private String msg(String chave) {
        return messageSource.getMessage(chave, null, PT_BR);
    }

    @Test
    @DisplayName("anônimo acessa a tela de login sem mensagens")
    void anonimoAcessaLoginSemMensagens() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/Login"))
                .andExpect(model().attributeDoesNotExist("error", "success"));
    }

    @Test
    @DisplayName("parâmetro error exibe mensagem de credenciais inválidas")
    void parametroErrorExibeCredenciaisInvalidas() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/Login"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_USER_INVALID_CREDENTIALS)));
    }

    @Test
    @DisplayName("parâmetro logout exibe mensagem de sucesso")
    void parametroLogoutExibeSucesso() throws Exception {
        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("success", msg(AppMessages.SUCCESS_USER_LOGOUT)))
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Test
    @DisplayName("parâmetro expired exibe mensagem de sessão encerrada")
    void parametroExpiredExibeSessaoEncerrada() throws Exception {
        mockMvc.perform(get("/login").param("expired", "true"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_SESSION_EXPIRED)));
    }

    @Test
    @DisplayName("error e expired juntos: prevalece a mensagem de sessão encerrada")
    void errorEExpiredJuntos() throws Exception {
        mockMvc.perform(get("/login").param("error", "true").param("expired", "true"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_SESSION_EXPIRED)));
    }

    @Test
    @DisplayName("usuário já autenticado continua recebendo a tela (permitAll, sem redirect)")
    void autenticadoRecebeTela() throws Exception {
        mockMvc.perform(get("/login").with(logadoComo(1L, "Admin", Descricao.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/Login"));
    }
}
