package br.com.lumiflow.security;

import br.com.lumiflow.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;

/**
 * Testa a SecurityConfig real (cadeia de filtros, hierarquia de perfis, CSRF, headers, login e
 * rate limit) sem banco e sem os controllers da aplicacao: um controller "pega-tudo" responde 200
 * quando a autorizacao libera a rota. Assim: 200 = autorizado, 403 = negado, 302 para /login = anonimo.
 */
@SpringJUnitWebConfig(SecurityConfigAuthorizationTest.TestConfig.class)
@DisplayName("SecurityConfig - autorizacao, CSRF, login e abuso")
class SecurityConfigAuthorizationTest {

    private static final String SENHA = "Senha123";
    private static final List<String> TODOS = List.of("ADMIN", "SUPERVISOR", "PCP", "GESTAO", "OPERADOR");

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, CustomAuthenticationSuccessHandler.class, AuthenticationEventsListener.class})
    static class TestConfig {

        @Bean
        LoginIpThrottle loginIpThrottle() {
            return new LoginIpThrottle(3, 15);
        }

        @Bean
        LoginAttemptService loginAttemptService() {
            return Mockito.mock(LoginAttemptService.class);
        }

        @Bean
        AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
            return new DefaultAuthenticationEventPublisher(publisher);
        }

        @Bean
        UserDetailsService userDetailsService() {
            PasswordEncoder encoder = new BCryptPasswordEncoder(4);
            String hash = encoder.encode(SENHA);
            return new InMemoryUserDetailsManager(
                    User.withUsername("operador").password(hash).roles("OPERADOR").build(),
                    User.withUsername("pcp").password(hash).roles("PCP").build(),
                    User.withUsername("trancado").password(hash).roles("OPERADOR").accountLocked(true).build()
            );
        }

        @Bean
        PegaTudo pegaTudo() {
            return new PegaTudo();
        }
    }

    @RestController
    public static class PegaTudo {
        @RequestMapping("/**")
        public String qualquer() {
            return "ok";
        }
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private LoginAttemptService loginAttemptService;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        reset(loginAttemptService);
    }

    // ------------------------------------------------------------------ helpers

    private MockHttpServletRequestBuilder req(String metodo, String caminho) {
        return request(HttpMethod.valueOf(metodo), caminho);
    }

    private MockHttpServletRequestBuilder login(String login, String senha, String ip) {
        return post("/login").servletPath("/login")
                .param("login", login).param("senha", senha)
                .with(csrf())
                .with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                });
    }

    // ------------------------------------------------------------------ matriz de autorizacao

    private static void regra(List<Arguments> saida, String metodo, String caminho, String... permitidos) {
        Set<String> ok = Set.of(permitidos);
        for (String perfil : TODOS) {
            saida.add(Arguments.of(metodo, caminho, perfil, ok.contains(perfil)));
        }
    }

    static Stream<Arguments> matriz() {
        List<Arguments> a = new ArrayList<>();
        // ADMIN exclusivo
        regra(a, "GET", "/dashboard/usuario/lista", "ADMIN");
        regra(a, "GET", "/dashboard/usuario", "ADMIN");
        regra(a, "POST", "/dashboard/usuario/salvar", "ADMIN");
        // configuracoes
        regra(a, "GET", "/dashboard/configuracao/geral", "ADMIN", "SUPERVISOR", "PCP", "GESTAO");
        regra(a, "GET", "/dashboard/configuracoes/geral", "ADMIN", "SUPERVISOR", "PCP", "GESTAO");
        // cadastros: PCP (Supervisor/Admin por heranca)
        for (String base : List.of("setores", "maquinas", "produtos", "roteiros", "operadores")) {
            regra(a, "GET", "/dashboard/" + base + "/lista", "ADMIN", "SUPERVISOR", "PCP");
            regra(a, "POST", "/dashboard/" + base + "/salvar", "ADMIN", "SUPERVISOR", "PCP");
        }
        // criacao de ordens
        regra(a, "GET", "/dashboard/ordens/novaOrdem", "ADMIN", "SUPERVISOR", "PCP");
        regra(a, "POST", "/dashboard/ordens", "ADMIN", "SUPERVISOR", "PCP");
        // liberacao / divisao / pausa
        regra(a, "POST", "/dashboard/ordens-setor/1/ordens/2/liberar", "ADMIN", "SUPERVISOR", "PCP", "GESTAO");
        regra(a, "POST", "/dashboard/ordens-setor/1/ordens/2/dividir", "ADMIN", "SUPERVISOR", "PCP", "GESTAO");
        regra(a, "POST", "/dashboard/ordens-setor/1/ordens/2/pausar", "ADMIN", "SUPERVISOR", "PCP", "GESTAO");
        // ordens por setor (consulta): qualquer autenticado
        regra(a, "GET", "/dashboard/ordens-setor/1", TODOS.toArray(String[]::new));
        // demais rotas de ordens (subrotas futuras, qualquer verbo): PCP por padrao (Supervisor/Admin por heranca)
        regra(a, "GET", "/dashboard/ordens/lista", "ADMIN", "SUPERVISOR", "PCP");
        regra(a, "GET", "/dashboard/ordens/5/detalhes", "ADMIN", "SUPERVISOR", "PCP", "GESTAO");
        // lancamento: operador e acima
        regra(a, "GET", "/dashboard/ordens/5/setor/2/lancar/form", TODOS.toArray(String[]::new));
        regra(a, "POST", "/dashboard/ordens/5/setor/2/lancar/salvar", TODOS.toArray(String[]::new));
        // vidracaria, relatorios, dashboard
        regra(a, "GET", "/dashboard/vidracaria/estoque", TODOS.toArray(String[]::new));
        regra(a, "GET", "/dashboard/relatorios/producao", "ADMIN", "SUPERVISOR", "PCP", "GESTAO");
        regra(a, "GET", "/dashboard", "ADMIN", "SUPERVISOR", "PCP", "GESTAO");
        regra(a, "GET", "/dashboard/outra-tela", TODOS.toArray(String[]::new));
        // default: qualquer outra rota exige autenticacao
        regra(a, "GET", "/rota-qualquer", TODOS.toArray(String[]::new));
        return a.stream();
    }

    @ParameterizedTest(name = "{0} {1} como {2} -> permitido={3}")
    @MethodSource("matriz")
    @DisplayName("matriz de autorizacao por rota e perfil (inclui hierarquia de perfis)")
    void matrizDeAutorizacao(String metodo, String caminho, String perfil, boolean permitido) throws Exception {
        mvc.perform(req(metodo, caminho).with(csrf()).with(user("u").roles(perfil)))
                .andExpect(status().is(permitido ? 200 : 403));
    }

    // ------------------------------------------------------------------ anonimo

    @ParameterizedTest(name = "anonimo GET {0} -> redireciona ao login")
    @ValueSource(strings = {
            "/dashboard", "/dashboard/usuario/lista", "/dashboard/setores/lista", "/dashboard/ordens/lista",
            "/dashboard/ordens-setor/1", "/dashboard/relatorios/x", "/dashboard/vidracaria/x",
            "/qualquer-coisa", "/actuator/env", "/actuator/info", "/actuator/metrics", "/actuator"
    })
    @DisplayName("anonimo em rota protegida e redirecionado para /login")
    void anonimoRedirecionaParaLogin(String caminho) throws Exception {
        mvc.perform(get(caminho))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @ParameterizedTest(name = "anonimo POST {0} com CSRF -> login")
    @ValueSource(strings = {"/dashboard/ordens", "/dashboard/usuario/salvar", "/dashboard/ordens-setor/1/ordens/2/liberar"})
    @DisplayName("anonimo com CSRF valido em POST protegido tambem vai para o login")
    void anonimoPostComCsrf(String caminho) throws Exception {
        mvc.perform(post(caminho).with(csrf()))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @ParameterizedTest(name = "anonimo POST {0} sem CSRF -> 403")
    @ValueSource(strings = {"/dashboard/ordens", "/dashboard/usuario/salvar", "/qualquer"})
    @DisplayName("CSRF ausente: POST anonimo sem token e rejeitado com 403 antes da autorizacao")
    void anonimoPostSemCsrf(String caminho) throws Exception {
        mvc.perform(post(caminho)).andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "publico: GET {0}")
    @ValueSource(strings = {"/login", "/css/app.css", "/js/app.js", "/images/logo.png", "/actuator/health",
            "/actuator/health/liveness"})
    @DisplayName("rotas publicas liberadas para anonimo")
    void rotasPublicas(String caminho) throws Exception {
        mvc.perform(get(caminho)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("prefixo parecido com rota publica nao abre acesso (/cssx, /actuator/healthz)")
    void prefixoParecidoNaoLibera() throws Exception {
        mvc.perform(get("/cssx/a.css")).andExpect(status().isFound());
        mvc.perform(get("/actuator/healthz")).andExpect(status().isFound());
        mvc.perform(get("/loginx")).andExpect(status().isFound());
    }

    // ------------------------------------------------------------------ CSRF

    @Test
    @DisplayName("CSRF ausente: usuario autenticado com permissao recebe 403 em POST sem token")
    void autenticadoSemCsrf() throws Exception {
        mvc.perform(post("/dashboard/ordens").with(user("u").roles("PCP")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF invalido: token forjado e rejeitado com 403")
    void csrfInvalido() throws Exception {
        mvc.perform(post("/dashboard/ordens").with(csrf().useInvalidToken()).with(user("u").roles("PCP")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("CSRF valido: usuario autorizado passa")
    void csrfValido() throws Exception {
        mvc.perform(post("/dashboard/ordens").with(csrf()).with(user("u").roles("PCP")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CSRF ausente no login: POST /login sem token e rejeitado e nao conta falha de senha")
    void loginSemCsrf() throws Exception {
        mvc.perform(post("/login").servletPath("/login").param("login", "operador").param("senha", "errada"))
                .andExpect(status().isForbidden());

        verify(loginAttemptService, never()).registrarFalha(any());
    }

    @Test
    @DisplayName("CSRF ausente no logout: POST /logout sem token e rejeitado")
    void logoutSemCsrf() throws Exception {
        mvc.perform(post("/logout").with(user("u").roles("OPERADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("logout com CSRF redireciona para /login?logout=true e invalida a sessao")
    void logoutComCsrf() throws Exception {
        MockHttpSession sessao = new MockHttpSession();

        mvc.perform(post("/logout").session(sessao).with(csrf()).with(user("u").roles("OPERADOR")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?logout=true"));

        assertThat(sessao.isInvalid()).isTrue();
    }

    // ------------------------------------------------------------------ login

    @Test
    @DisplayName("login de operador redireciona para /dashboard/ordens-setor")
    void loginOperador() throws Exception {
        mvc.perform(login("operador", SENHA, "10.10.0.1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard/ordens-setor"))
                .andExpect(authenticated().withUsername("operador"));

        verify(loginAttemptService, atLeastOnce()).registrarSucesso("operador");
    }

    @Test
    @DisplayName("login de PCP redireciona para /dashboard")
    void loginPcp() throws Exception {
        mvc.perform(login("pcp", SENHA, "10.10.0.2"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("senha errada redireciona para /login?error=true e registra falha na conta")
    void senhaErrada() throws Exception {
        mvc.perform(login("operador", "errada", "10.10.0.3"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());

        verify(loginAttemptService, atLeastOnce()).registrarFalha("operador");
        verify(loginAttemptService, never()).registrarSucesso(any());
    }

    @Test
    @DisplayName("enumeracao de usuarios: login inexistente responde igual a senha errada")
    void usuarioInexistenteIgualASenhaErrada() throws Exception {
        MvcResult inexistente = mvc.perform(login("fantasma", "qualquer", "10.10.0.4")).andReturn();
        MvcResult senhaErrada = mvc.perform(login("operador", "errada", "10.10.0.5")).andReturn();

        assertThat(inexistente.getResponse().getStatus()).isEqualTo(senhaErrada.getResponse().getStatus());
        assertThat(inexistente.getResponse().getRedirectedUrl()).isEqualTo(senhaErrada.getResponse().getRedirectedUrl());
    }

    @Test
    @DisplayName("conta bloqueada nao autentica nem com a senha correta")
    void contaBloqueada() throws Exception {
        mvc.perform(login("trancado", SENHA, "10.10.0.6"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());

        verify(loginAttemptService, never()).registrarSucesso(any());
    }

    @Test
    @DisplayName("session fixation: sessao anterior ao login e invalidada e o id muda")
    void sessionFixation() throws Exception {
        MockHttpSession antiga = new MockHttpSession();
        String idAntigo = antiga.getId();

        MvcResult resultado = mvc.perform(login("operador", SENHA, "10.10.0.7").session(antiga)).andReturn();

        assertThat(antiga.isInvalid()).isTrue();
        assertThat(resultado.getRequest().getSession(false)).isNotNull();
        assertThat(resultado.getRequest().getSession(false).getId()).isNotEqualTo(idAntigo);
    }

    @Test
    @DisplayName("redirect aberto: parametros de retorno no login nao alteram o destino")
    void loginIgnoraRedirectForjado() throws Exception {
        mvc.perform(login("operador", SENHA, "10.10.0.8")
                        .param("redirect", "http://evil.example.com")
                        .param("next", "//evil.example.com")
                        .header("Referer", "http://evil.example.com"))
                .andExpect(redirectedUrl("/dashboard/ordens-setor"));
    }

    // ------------------------------------------------------------------ brute force / rate limit

    @Test
    @DisplayName("brute force: apos o limite de falhas o IP e barrado ate com a senha correta")
    void bruteForceBarraIp() throws Exception {
        String ip = "10.20.0.1";
        for (int i = 0; i < 3; i++) {
            mvc.perform(login("operador", "errada" + i, ip)).andExpect(redirectedUrl("/login?error=true"));
        }
        reset(loginAttemptService);

        mvc.perform(login("operador", SENHA, ip))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());

        // Barrado pelo filtro antes de qualquer verificacao de senha/consulta ao banco.
        verify(loginAttemptService, never()).registrarSucesso(any());
        verify(loginAttemptService, never()).registrarFalha(any());
    }

    @Test
    @DisplayName("password spraying: varios logins distintos do mesmo IP tambem esgotam o limite")
    void passwordSpraying() throws Exception {
        String ip = "10.20.0.2";
        for (String alvo : List.of("alvo1", "alvo2", "alvo3")) {
            mvc.perform(login(alvo, "Senha123", ip));
        }

        mvc.perform(login("operador", SENHA, ip))
                .andExpect(redirectedUrl("/login?error=true"))
                .andExpect(unauthenticated());
    }

    @Test
    @DisplayName("X-Forwarded-For forjado nao contorna o bloqueio por IP")
    void xForwardedForNaoContorna() throws Exception {
        String ip = "10.20.0.3";
        for (int i = 0; i < 3; i++) {
            mvc.perform(login("operador", "errada", ip));
        }

        for (int i = 0; i < 3; i++) {
            mvc.perform(login("operador", SENHA, ip).header("X-Forwarded-For", "198.51.100." + i))
                    .andExpect(redirectedUrl("/login?error=true"))
                    .andExpect(unauthenticated());
        }
    }

    @Test
    @DisplayName("bloqueio por IP nao afeta outro IP")
    void bloqueioIsoladoPorIp() throws Exception {
        String atacante = "10.20.0.4";
        for (int i = 0; i < 3; i++) {
            mvc.perform(login("operador", "errada", atacante));
        }

        mvc.perform(login("operador", SENHA, "10.20.0.5"))
                .andExpect(redirectedUrl("/dashboard/ordens-setor"));
    }

    @Test
    @DisplayName("falhas abaixo do limite nao impedem login correto")
    void abaixoDoLimiteNaoBloqueia() throws Exception {
        String ip = "10.20.0.6";
        mvc.perform(login("operador", "errada", ip));

        mvc.perform(login("operador", SENHA, ip)).andExpect(redirectedUrl("/dashboard/ordens-setor"));
    }

    // ------------------------------------------------------------------ headers

    @Test
    @DisplayName("headers de seguranca: CSP restritiva, Referrer-Policy, nosniff e X-Frame-Options")
    void headersDeSeguranca() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("script-src 'self';")))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("object-src 'none'")))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("frame-ancestors 'none'")))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("unsafe-inline"))))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    // ------------------------------------------------------------------ endurecimento de rotas

    @Test
    @DisplayName("verbos nao mapeados em /dashboard/ordens/** nao vazam: OPERADOR recebe 403 (deny-by-default por perfil)")
    void verbosNaoListadosNaoVazam() throws Exception {
        mvc.perform(req("PUT", "/dashboard/ordens").with(csrf()).with(user("u").roles("OPERADOR")))
                .andExpect(status().isForbidden());
        mvc.perform(req("DELETE", "/dashboard/ordens/5").with(csrf()).with(user("u").roles("OPERADOR")))
                .andExpect(status().isForbidden());
        mvc.perform(req("POST", "/dashboard/ordens/5/detalhes").with(csrf()).with(user("u").roles("OPERADOR")))
                .andExpect(status().isForbidden());
        mvc.perform(req("GET", "/dashboard/ordens-setor/1/ordens/2/liberar").with(user("u").roles("OPERADOR")))
                .andExpect(status().isForbidden());
    }
}
