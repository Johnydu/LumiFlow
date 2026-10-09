package br.com.lumiflow.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * CONFIGURAÇÃO DE SEGURANÇA — LumiFlow
 * <p>
 * HIERARQUIA DE PERFIS:
 * - ADMIN: acesso total
 * - SUPERVISOR: engloba PCP e Gestão
 * - PCP / GESTÃO: planejamento e liberação/relatórios
 * - OPERADOR: lançamentos no chão de fábrica
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
    private static final String ROLE_SUPERVISOR = "SUPERVISOR";
    private static final String ROLE_GESTAO = "GESTAO";
    private static final String ROLE_OPERADOR = "OPERADOR";

    private static final String LOGIN_PAGE = "/login";

    /**
     * CSP em modo "report-only": não bloqueia nada, apenas relata no console do
     * navegador o que seria bloqueado. Os templates ainda usam scripts, estilos
     * e handlers inline ('unsafe-inline'). Depois de remover os inline, endureça
     * e troque para enforcement.
     */
    private static final String CSP_POLICY = "default-src 'self'; "
            + "script-src 'self' 'unsafe-inline'; "
            + "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; "
            + "font-src 'self' https://cdnjs.cloudflare.com; "
            + "img-src 'self' data:; "
            + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy(
                "ROLE_ADMIN > ROLE_SUPERVISOR\n" +
                        "ROLE_SUPERVISOR > ROLE_PCP\n" +
                        "ROLE_SUPERVISOR > ROLE_GESTAO\n" +
                        "ROLE_PCP > ROLE_OPERADOR\n" +
                        "ROLE_GESTAO > ROLE_OPERADOR"
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // Health check público para o container e a plataforma de deploy
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // Recursos estáticos
                        .requestMatchers("/css/**", "/js/**", "/image/**", "/images/**",
                                "/style.css", "/webjars/**").permitAll()

                        // Tela de login
                        .requestMatchers(LOGIN_PAGE).permitAll()

                        // ADMIN: usuários e configurações
                        // (inclui as rotas no singular usadas pelo menu lateral)
                        .requestMatchers("/dashboard/usuario/**").hasRole(ROLE_ADMIN)
                        .requestMatchers("/dashboard/configuracao/**",
                                "/dashboard/configuracoes/**").authenticated()

                        // Cadastros e criação de ordens: PCP (Supervisor e Admin por herança)
                        .requestMatchers(
                                "/dashboard/setores/**",
                                "/dashboard/maquinas/**",
                                "/dashboard/produtos/**",
                                "/dashboard/roteiros/**",
                                "/dashboard/operadores/**"
                        ).hasRole(ROLE_PCP)

                        .requestMatchers(HttpMethod.GET, "/dashboard/ordens/novaOrdem").hasRole(ROLE_PCP)
                        .requestMatchers(HttpMethod.POST, "/dashboard/ordens").hasRole(ROLE_PCP)

                        // Liberação e divisão de ordens: Gestão
                        .requestMatchers(HttpMethod.POST, "/dashboard/ordens-setor/*/ordens/*/liberar")
                        .hasAnyRole(ROLE_GESTAO, ROLE_PCP, ROLE_SUPERVISOR)
                        .requestMatchers(HttpMethod.POST, "/dashboard/ordens-setor/*/ordens/*/dividir")
                        .hasAnyRole(ROLE_GESTAO, ROLE_PCP, ROLE_SUPERVISOR)

                        // Ordens por setor: autenticados
                        .requestMatchers("/dashboard/ordens-setor/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/dashboard/ordens/*/detalhes")
                        .hasAnyRole(ROLE_GESTAO, ROLE_PCP)

                        // Lançamento de produção: Operador e acima
                        .requestMatchers("/dashboard/ordens/*/setor/*/lancar/**").hasRole(ROLE_OPERADOR)

                        // Demais rotas de ordens: autenticados
                        .requestMatchers("/dashboard/ordens/**").authenticated()

                        // Vidraçaria: autenticados
                        .requestMatchers("/dashboard/vidracaria/**").authenticated()

                        .requestMatchers("/dashboard/relatorios/**")
                        .hasAnyRole(ROLE_GESTAO, ROLE_PCP, ROLE_SUPERVISOR)

                        // Dashboard (raiz)
                        .requestMatchers("/dashboard").hasAnyRole(ROLE_GESTAO, ROLE_PCP, ROLE_SUPERVISOR)

                        // Regras genéricas (devem vir após as específicas)
                        .requestMatchers("/dashboard/**").authenticated()

                        // Negar por padrão: qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )

                // Limite de falhas por IP, antes da autenticação por formulário
                .addFilterBefore(new LoginRateLimitFilter(loginIpThrottle),
                        UsernamePasswordAuthenticationFilter.class)

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
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession())
                )

                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY).reportOnly())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )

                .csrf(Customizer.withDefaults());

        return http.build();
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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}