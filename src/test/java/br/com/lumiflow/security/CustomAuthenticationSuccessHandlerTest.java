package br.com.lumiflow.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomAuthenticationSuccessHandler")
class CustomAuthenticationSuccessHandlerTest {

    private final CustomAuthenticationSuccessHandler handler = new CustomAuthenticationSuccessHandler();

    private Authentication auth(String... roles) {
        return UsernamePasswordAuthenticationToken.authenticated("u", "n/a", AuthorityUtils.createAuthorityList(roles));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "ROLE_ADMIN, /dashboard",
            "ROLE_SUPERVISOR, /dashboard",
            "ROLE_PCP, /dashboard",
            "ROLE_GESTAO, /dashboard",
            "ROLE_OPERADOR, /dashboard/ordens-setor",
            "ROLE_USER, /dashboard/ordens-setor"
    })
    @DisplayName("deve redirecionar por perfil")
    void redirecionaPorPerfil(String role, String destino) throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), resp, auth(role));

        assertThat(resp.getRedirectedUrl()).isEqualTo(destino);
    }

    @Test
    @DisplayName("sem nenhuma authority deve ir para a tela de ordens por setor")
    void semAuthorities() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), resp, auth());

        assertThat(resp.getRedirectedUrl()).isEqualTo("/dashboard/ordens-setor");
    }

    @Test
    @DisplayName("perfil com mais de uma authority privilegiada vai para o dashboard")
    void multiplasAuthorities() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), resp, auth("ROLE_OPERADOR", "ROLE_GESTAO"));

        assertThat(resp.getRedirectedUrl()).isEqualTo("/dashboard");
    }

    @Test
    @DisplayName("nome de role sem prefixo ROLE_ nao concede acesso ao dashboard")
    void roleSemPrefixoNaoConcede() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), resp, auth("ADMIN"));

        assertThat(resp.getRedirectedUrl()).isEqualTo("/dashboard/ordens-setor");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://evil.example.com",
            "//evil.example.com",
            "https://evil.example.com/phish",
            "/\\evil.example.com"
    })
    @DisplayName("redirect aberto: parametros de retorno e Referer forjados sao ignorados")
    void naoHaRedirectAberto(String alvo) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        for (String nome : new String[]{"redirect", "redirect_uri", "next", "continue", "returnUrl", "url", "target"}) {
            req.setParameter(nome, alvo);
        }
        req.addHeader("Referer", alvo);
        req.addHeader("X-Forwarded-Host", "evil.example.com");
        req.addHeader("Host", "evil.example.com");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(req, resp, auth("ROLE_ADMIN"));

        assertThat(resp.getRedirectedUrl()).isEqualTo("/dashboard");
    }

    @Test
    @DisplayName("redirect aberto: SavedRequest forjada na sessao nao influencia o destino")
    void savedRequestIgnorada() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_SAVED_REQUEST", "http://evil.example.com");
        req.setSession(session);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(req, resp, auth("ROLE_OPERADOR"));

        assertThat(resp.getRedirectedUrl()).isEqualTo("/dashboard/ordens-setor");
    }

    @Test
    @DisplayName("destino e sempre caminho relativo local (comeca com uma unica barra)")
    void destinoSempreLocal() throws Exception {
        for (String role : new String[]{"ROLE_ADMIN", "ROLE_OPERADOR", "ROLE_XYZ"}) {
            MockHttpServletResponse resp = new MockHttpServletResponse();
            handler.onAuthenticationSuccess(new MockHttpServletRequest(), resp, auth(role));
            assertThat(resp.getRedirectedUrl()).startsWith("/").doesNotStartWith("//").doesNotContain("://");
        }
    }
}
