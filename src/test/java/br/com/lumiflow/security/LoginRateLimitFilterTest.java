package br.com.lumiflow.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginRateLimitFilter")
class LoginRateLimitFilterTest {

    @Mock
    private LoginIpThrottle throttle;
    @Mock
    private FilterChain chain;

    private MockHttpServletRequest request(String metodo, String caminho, String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest(metodo, caminho);
        req.setServletPath(caminho);
        req.setRemoteAddr(ip);
        return req;
    }

    @Test
    @DisplayName("POST /login de IP bloqueado deve redirecionar para erro generico sem seguir a cadeia")
    void ipBloqueadoRedireciona() throws Exception {
        when(throttle.estaBloqueado("5.5.5.5")).thenReturn(true);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        new LoginRateLimitFilter(throttle).doFilter(request("POST", "/login", "5.5.5.5"), resp, chain);

        assertThat(resp.getRedirectedUrl()).isEqualTo("/login?error=true");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("redirecionamento deve respeitar o context path")
    void respeitaContextPath() throws Exception {
        when(throttle.estaBloqueado("5.5.5.5")).thenReturn(true);
        MockHttpServletRequest req = request("POST", "/login", "5.5.5.5");
        req.setContextPath("/app");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        new LoginRateLimitFilter(throttle).doFilter(req, resp, chain);

        assertThat(resp.getRedirectedUrl()).isEqualTo("/app/login?error=true");
    }

    @Test
    @DisplayName("POST /login de IP liberado deve seguir a cadeia")
    void ipLiberadoSegue() throws Exception {
        when(throttle.estaBloqueado("5.5.5.5")).thenReturn(false);
        MockHttpServletRequest req = request("POST", "/login", "5.5.5.5");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        new LoginRateLimitFilter(throttle).doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
        assertThat(resp.getRedirectedUrl()).isNull();
    }

    @Test
    @DisplayName("metodo em minusculas tambem e filtrado")
    void metodoCaseInsensitive() throws Exception {
        when(throttle.estaBloqueado("5.5.5.5")).thenReturn(true);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        new LoginRateLimitFilter(throttle).doFilter(request("post", "/login", "5.5.5.5"), resp, chain);

        assertThat(resp.getRedirectedUrl()).isEqualTo("/login?error=true");
    }

    @Test
    @DisplayName("GET /login nao e filtrado mesmo para IP bloqueado (permite exibir a tela)")
    void getLoginNaoFiltrado() throws Exception {
        MockHttpServletRequest req = request("GET", "/login", "5.5.5.5");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        new LoginRateLimitFilter(throttle).doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
        verifyNoInteractions(throttle);
    }

    @Test
    @DisplayName("POST em outra rota nao consulta o throttle")
    void outraRotaNaoFiltrada() throws Exception {
        MockHttpServletRequest req = request("POST", "/dashboard/ordens", "5.5.5.5");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        new LoginRateLimitFilter(throttle).doFilter(req, resp, chain);

        verify(chain).doFilter(req, resp);
        verifyNoInteractions(throttle);
    }

    @Test
    @DisplayName("X-Forwarded-For forjado deve ser ignorado: vale apenas getRemoteAddr")
    void xForwardedForForjadoIgnorado() throws Exception {
        when(throttle.estaBloqueado("5.5.5.5")).thenReturn(true);
        MockHttpServletRequest req = request("POST", "/login", "5.5.5.5");
        req.addHeader("X-Forwarded-For", "203.0.113.77");
        req.addHeader("X-Real-IP", "203.0.113.78");
        req.addHeader("Forwarded", "for=203.0.113.79");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        new LoginRateLimitFilter(throttle).doFilter(req, resp, chain);

        assertThat(resp.getRedirectedUrl()).isEqualTo("/login?error=true");
        verify(throttle).estaBloqueado("5.5.5.5");
        verify(throttle, never()).estaBloqueado("203.0.113.77");
        verify(throttle, never()).estaBloqueado("203.0.113.78");
        verify(throttle, never()).estaBloqueado("203.0.113.79");
    }

    @Test
    @DisplayName("rotacionar X-Forwarded-For a cada tentativa nao contorna o bloqueio (throttle real)")
    void rotacaoDeXffNaoContornaBloqueio() throws Exception {
        LoginIpThrottle real = new LoginIpThrottle(2, 15);
        real.registrarFalha("7.7.7.7");
        real.registrarFalha("7.7.7.7");
        LoginRateLimitFilter filtro = new LoginRateLimitFilter(real);

        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest req = request("POST", "/login", "7.7.7.7");
            req.addHeader("X-Forwarded-For", "198.51.100." + i);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            filtro.doFilter(req, resp, chain);
            assertThat(resp.getRedirectedUrl()).isEqualTo("/login?error=true");
        }
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("resposta de bloqueio e identica a de falha comum (nao revela o rate limit)")
    void respostaGenerica() throws Exception {
        when(throttle.estaBloqueado("5.5.5.5")).thenReturn(true);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        new LoginRateLimitFilter(throttle).doFilter(request("POST", "/login", "5.5.5.5"), resp, chain);

        assertThat(resp.getStatus()).isEqualTo(302);
        assertThat(resp.getRedirectedUrl()).isEqualTo("/login?error=true");
        assertThat(resp.getContentAsString()).isEmpty();
    }
}
