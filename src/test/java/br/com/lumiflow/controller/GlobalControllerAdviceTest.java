package br.com.lumiflow.controller;

import br.com.lumiflow.dto.usuario.UsuarioLogadoDTO;
import br.com.lumiflow.entity.enums.Descricao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static br.com.lumiflow.controller.ControllerTestSupport.logadoComo;
import static br.com.lumiflow.controller.ControllerTestSupport.usuarioDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@WebMvcTest(controllers = LoginController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(LoginController.class)
@DisplayName("GlobalControllerAdvice")
class GlobalControllerAdviceTest {

    @Autowired
    MockMvc mockMvc;

    private final GlobalControllerAdvice advice = new GlobalControllerAdvice();

    @Test
    @DisplayName("expõe usuarioLogado no model quando o principal é UsuarioDetails")
    void expoeUsuarioLogado() throws Exception {
        mockMvc.perform(get("/login").with(logadoComo(1L, "Maria", Descricao.ADMIN)))
                .andExpect(model().attribute("usuarioLogado", new UsuarioLogadoDTO("Maria", "ADMIN")));
    }

    @Test
    @DisplayName("usuarioLogado é nulo para anônimo")
    void nuloParaAnonimo() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(model().attribute("usuarioLogado", nullValue()));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("usuarioLogado é nulo quando o principal não é UsuarioDetails")
    void nuloQuandoPrincipalNaoEUsuarioDetails() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(model().attribute("usuarioLogado", nullValue()));
    }

    @Test
    @DisplayName("unitário: authentication nulo retorna nulo")
    void authenticationNulo() {
        assertThat(advice.usuarioLogado(null)).isNull();
    }

    @Test
    @DisplayName("unitário: authentication não autenticado retorna nulo")
    void naoAutenticado() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        assertThat(advice.usuarioLogado(auth)).isNull();
    }

    @Test
    @DisplayName("unitário: principal UsuarioDetails vira UsuarioLogadoDTO")
    void principalUsuarioDetails() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(usuarioDetails(2L, "Joao", Descricao.OPERADOR));
        assertThat(advice.usuarioLogado(auth)).isEqualTo(new UsuarioLogadoDTO("Joao", "OPERADOR"));
    }

    @Test
    @DisplayName("unitário: principal String (anonymousUser) retorna nulo")
    void principalString() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("anonymousUser");
        assertThat(advice.usuarioLogado(auth)).isNull();
    }

    @Test
    @DisplayName("unitário: exceção ao ler o principal é engolida e retorna nulo")
    void excecaoEngolida() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenThrow(new IllegalStateException("falha"));
        assertThat(advice.usuarioLogado(auth)).isNull();
    }
}
