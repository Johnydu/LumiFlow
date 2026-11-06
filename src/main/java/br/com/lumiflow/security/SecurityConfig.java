package br.com.lumiflow.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;

/**
 * CONFIGURAÇÃO DE SEGURANÇA — LumiFlow
 * <p>
 * HIERARQUIA DE PERFIS (ADMIN > SUPERVISOR > PCP/GESTÃO > OPERADOR):
 * ADMIN: acesso total; é o único perfil que gere utilizadores
 * SUPERVISOR: engloba PCP e Gestão
 * PCP / GESTÃO: planejamento, liberação de ordens, relatórios e configurações
 * OPERADOR: lançamentos no chão de fábrica
 * <p>
 * Como a hierarquia já propaga as permissões, as regras usam o menor perfil necessário
 * (ex.: hasRole(PCP) já vale para SUPERVISOR e ADMIN). Cada regra de URL tem o mesmo critério do
 * @PreAuthorize do controller correspondente (defesa em profundidade).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final LoginIpThrottle loginIpThrottle;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_PCP = "PCP";
    private static final String ROLE_GESTAO = "GESTAO";
    private static final String ROLE_OPERADOR = "OPERADOR";

    private static final String LOGIN_PAGE = "/login";

    /** HSTS: 1 ano. Só é emitido pelo Spring Security em requisições HTTPS. */
    private static final long HSTS_MAX_AGE_SEGUNDOS = 31_536_000L;

    /** Recursos de hardware que a aplicação não usa ficam desligados para qualquer origem. */
    private static final String PERMISSIONS_POLICY =
            "camera=(), microphone=(), geolocation=(), payment=(), usb=(), interest-cohort=()";

    /**
     * CSP em modo de imposição (enforcement) ativa.
     * Os blocos inline foram eliminados dos modelos, permitindo bloquear estritamente
     * scripts e estilos não autorizados para blindagem contra XSS.
     */
    private static final String CSP_POLICY = "default-src 'self'; "
            + "script-src 'self'; "
            + "style-src 'self' https://cdnjs.cloudflare.com; "
            + "font-src 'self' https://cdnjs.cloudflare.com; "
            + "img-src 'self' data:; "
            + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_SUPERVISOR
                ROLE_SUPERVISOR > ROLE_PCP
                ROLE_SUPERVISOR > ROLE_GESTAO
                ROLE_PCP > ROLE_OPERADOR
                ROLE_GESTAO > ROLE_OPERADOR
                """);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // Health check público para o contentor e a plataforma de deploy
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // Recursos estáticos
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()

                        // Ecrã de login
                        .requestMatchers(LOGIN_PAGE).permitAll()

                        // ADMIN: somente gestão de utilizadores
                        .requestMatchers("/dashboard/usuario/**").hasRole(ROLE_ADMIN)

                        // Configurações: PCP e Gestão (Supervisor e Admin por herança)
                        .requestMatchers("/dashboard/configuracao/**",
                                "/dashboard/configuracoes/**")
                        .hasAnyRole(ROLE_GESTAO, ROLE_PCP)

                        // Cadastros: PCP (Supervisor e Admin por herança)
                        .requestMatchers(
                                "/dashboard/setores/**",
                                "/dashboard/maquinas/**",
                                "/dashboard/produtos/**",
                                "/dashboard/roteiros/**",
                                "/dashboard/operadores/**"
                        ).hasRole(ROLE_PCP)

                        // Criação de ordens: PCP, qualquer verbo (alinhado ao @PreAuthorize do controller)
                        .requestMatchers("/dashboard/ordens", "/dashboard/ordens/novaOrdem").hasRole(ROLE_PCP)

                        // Liberação, divisão e pausa de etapas (e qualquer subrota/verbo): Gestão e PCP
                        .requestMatchers("/dashboard/ordens-setor/*/ordens/**")
                        .hasAnyRole(ROLE_GESTAO, ROLE_PCP)

                        // Ordens por setor (consulta): autenticados
                        .requestMatchers("/dashboard/ordens-setor/**").authenticated()

                        // Detalhes da ordem: Gestão e PCP, qualquer verbo
                        .requestMatchers("/dashboard/ordens/*/detalhes").hasAnyRole(ROLE_GESTAO, ROLE_PCP)

                        // Lançamento de produção: Operador e acima
                        .requestMatchers("/dashboard/ordens/*/setor/*/lancar/**").hasRole(ROLE_OPERADOR)

                        // Demais rotas de ordens (inclui PUT/DELETE e subrotas futuras): PCP por padrão
                        .requestMatchers("/dashboard/ordens/**").hasRole(ROLE_PCP)

                        // Vidraçaria: autenticados
                        .requestMatchers("/dashboard/vidracaria/**").authenticated()

                        .requestMatchers("/dashboard/relatorios/**").hasAnyRole(ROLE_GESTAO, ROLE_PCP)

                        // Dashboard (raiz)
                        .requestMatchers("/dashboard").hasAnyRole(ROLE_GESTAO, ROLE_PCP)

                        // API interna: autenticados (anônimo recebe 401, ver exceptionHandling)
                        .requestMatchers("/api/**").authenticated()

                        // Regras genéricas (devem vir após as específicas)
                        .requestMatchers("/dashboard/**").authenticated()

                        // Negar por padrão: qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )

                // Limite de falhas por IP, antes da autenticação por formulário
                .addFilterBefore(new LoginRateLimitFilter(loginIpThrottle),
                        UsernamePasswordAuthenticationFilter.class)

                // /api/** (fetch/AJAX) recebe 401 em vez de redirect 302 para a página de login
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()))

                .formLogin(form -> form
                        .loginPage(LOGIN_PAGE)
                        .loginProcessingUrl(LOGIN_PAGE)
                        .usernameParameter("login")
                        .passwordParameter("senha")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl(LOGIN_PAGE + "?error=true")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl(LOGIN_PAGE + "?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // Explícito por clareza: novo ID de sessão a cada login (anti session fixation)
                // Sessões registradas (sem limite por usuário, pois o computador é compartilhado):
                // permite encerrar as sessões de quem foi excluído, rebaixado ou trocou de senha.
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession())
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(-1)
                                .sessionRegistry(sessionRegistry())
                                .expiredUrl(LOGIN_PAGE + "?expired=true"))
                )

                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(HSTS_MAX_AGE_SEGUNDOS))
                        .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", PERMISSIONS_POLICY))
                )

                .csrf(Customizer.withDefaults());

        return http.build();
    }

    /** /api/** responde 401 para anônimos; as demais rotas redirecionam para a tela de login. */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> porRota = new LinkedHashMap<>();
        porRota.put(PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(porRota);
        entryPoint.setDefaultEntryPoint(new LoginUrlAuthenticationEntryPoint(LOGIN_PAGE));
        return entryPoint;
    }

    /**
     * Custo 12: cada tentativa de senha fica mais cara para um atacante.
     * Hashes já gravados (custo 10) continuam válidos: o custo vai dentro do próprio hash.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /** Avisa o registro quando uma sessão HTTP expira ou é destruída. */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
