package br.com.lumiflow.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

/**
 * ============================================================
 * CONFIGURAÇÃO DE SEGURANÇA — LumiFlow
 * ============================================================
 * Responsável por:
 * - Definir as rotas protegidas e públicas com base nas Roles
 * - Configurar a Hierarquia de Perfis (Role Hierarchy)
 * - Configurar o formulário de login personalizado
 * - Configurar o logout e segurança de sessão
 * - Registrar o BCrypt como encoder de senhas
 * ============================================================

 * HIERARQUIA DE PERFIS ADOTADA:
 * - ADMIN: Acesso total (topo da hierarquia)
 * - SUPERVISOR: Engloba PCP, Gestão e Operador (gestão operacional ampla)
 * - PCP / GESTÃO: Acessos específicos de engenharia/planejamento e relatórios/liberação
 * - OPERADOR: Acesso base para lançamentos no chão de fábrica
 * ============================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permite o uso de @PreAuthorize nos controllers
@AllArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    // ============================================================
    //                      ROLES DO SISTEMA
    // ============================================================
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPERVISOR = "SUPERVISOR";
    private static final String ROLE_PCP = "PCP";
    private static final String ROLE_GESTAO = "GESTAO";
    private static final String ROLE_OPERADOR = "OPERADOR";

    private static final String LOGIN_PAGE = "/login";

    // ============================================================
    // HIERARQUIA DE PERFIS (ROLE HIERARCHY)
    // Define que o ADMIN herda tudo, o SUPERVISOR herda PCP e Gestão,
    // e ambos contemplam as camadas operacionais e de visualização.
    // ============================================================
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

    // ============================================================
    // FILTRO DE SEGURANÇA — DEFINE AS REGRAS DE ACESSO POR ROTA
    // ============================================================
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // Recursos estáticos — liberados para todos
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/image/**",
                                "/images/**",
                                "/style.css",
                                "/webjars/**"
                        ).permitAll()

                        // Tela de login — pública
                        .requestMatchers(LOGIN_PAGE, LOGIN_PAGE + "?error").permitAll()

                        // ----------------------------------------
                        // ROTAS EXCLUSIVAS DO ADMIN (Gestão de Sistema / Usuários)
                        // ----------------------------------------
                        .requestMatchers(
                                "/dashboard/usuario/**",
                                "/dashboard/configuracoes/**"
                        ).hasRole(ROLE_ADMIN)

                        // ----------------------------------------
                        // ROTAS DE CADASTRO E CRIAÇÃO DE ORDENS (PCP, Supervisor e Admin)
                        // ----------------------------------------
                        .requestMatchers(
                                "/dashboard/setores/**",
                                "/dashboard/maquinas/**",
                                "/dashboard/produtos/**",
                                "/dashboard/roteiros/**",
                                "/dashboard/operadores/**"
                        ).hasRole(ROLE_PCP) // Supervisor e Admin entram automaticamente por herança

                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/dashboard/ordens/novaOrdem")
                        .hasRole(ROLE_PCP)
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/dashboard/ordens")
                        .hasRole(ROLE_PCP)

                        // ----------------------------------------
                        // LIBERAÇÃO E DIVISÃO DE ORDENS (Gestão, PCP, Supervisor e Admin)
                        // ----------------------------------------
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/dashboard/ordens-setor/*/ordens/*/liberar")
                        .hasRole(ROLE_GESTAO) // Supervisor e Admin entram automaticamente
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/dashboard/ordens-setor/*/ordens/*/dividir")
                        .hasRole(ROLE_GESTAO)

                        // ----------------------------------------
                        // VISUALIZAÇÃO DE ORDENS POR SETOR — AUTENTICADOS
                        // ----------------------------------------
                        .requestMatchers("/dashboard/ordens-setor/**").authenticated()

                        // ----------------------------------------
                        // LANÇAMENTO DE PRODUÇÃO — OPERADORES E ACIMA
                        // ----------------------------------------
                        .requestMatchers("/dashboard/ordens/*/setor/*/lancar/**").hasRole(ROLE_OPERADOR)

                        // ----------------------------------------
                        // VISUALIZAÇÃO DE ORDENS — AUTENTICADOS
                        // ----------------------------------------
                        .requestMatchers("/dashboard/ordens/**").authenticated()

                        // ----------------------------------------
                        // VIDRAÇARIA — AUTENTICADOS
                        // ----------------------------------------
                        .requestMatchers("/dashboard/vidracaria/**").authenticated()

                        // ----------------------------------------
                        // RELATÓRIOS (Gestão, PCP, Supervisor, Admin)
                        // ----------------------------------------
                        .requestMatchers("/dashboard/relatorios/**")
                        .hasRole(ROLE_GESTAO)

                        // ----------------------------------------
                        // DASHBOARD (Tela Raiz) — Acessível por perfis gerenciais/técnicos
                        // ----------------------------------------
                        .requestMatchers("/dashboard").hasRole(ROLE_GESTAO)

                        // ----------------------------------------
                        // DASHBOARD — REGRAS GENÉRICAS (Devem vir após as específicas)
                        // ----------------------------------------
                        .requestMatchers("/dashboard/**").authenticated()

                        // Qualquer outra rota exige autenticação padrão
                        .anyRequest().authenticated()
                )

                // ----------------------------------------
                // CONFIGURAÇÃO DO FORMULÁRIO DE LOGIN
                // ----------------------------------------
                .formLogin(form -> form
                        .loginPage(LOGIN_PAGE)
                        .loginProcessingUrl(LOGIN_PAGE)
                        .usernameParameter("login")
                        .passwordParameter("senha")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl(LOGIN_PAGE + "?error=true")
                        .permitAll()
                )

                // ----------------------------------------
                // CONFIGURAÇÃO DO LOGOUT
                // ----------------------------------------
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl(LOGIN_PAGE + "?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(Customizer.withDefaults());

        return http.build();
    }

    // ============================================================
    // BCRYPT — ENCODER DE SENHAS
    // ============================================================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ============================================================
    // AUTHENTICATION MANAGER
    // ============================================================
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}