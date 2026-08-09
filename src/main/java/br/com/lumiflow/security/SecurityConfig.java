package br.com.lumiflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * - Definir as rotas protegidas e públicas
 * - Configurar o formulário de login personalizado
 * - Configurar o logout
 * - Registrar o BCrypt como encoder de senhas
 * ============================================================
 *
 * IMPORTANTE: os requestMatchers específicos (com hasRole/hasAnyRole)
 * precisam vir ANTES das regras genéricas (ex: "/dashboard/**"),
 * porque o Spring Security aplica a PRIMEIRA regra que casar com a URL.
 * Todas as rotas abaixo foram ajustadas para bater com o prefixo real
 * usado nos controllers: "/dashboard/...".
 * ============================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // permite usar @PreAuthorize nos controllers
public class SecurityConfig {

    // ============================================================
    // ROLES — evita duplicação de literais espalhadas pela config
    // ============================================================
    private static final String ROLE_SUPORTE = "SUPORTE";
    private static final String ROLE_PCP_SUPERVISOR = "PCP_SUPERVISOR";
    private static final String ROLE_GESTAO = "GESTAO";
    private static final String LOGIN_PAGE = "/login";

    // Observação: não precisamos injetar UserDetailsServiceImpl aqui.
    // Como ele é a única implementação de UserDetailsService no contexto,
    // o Spring Security a detecta e usa automaticamente no
    // DaoAuthenticationProvider por trás do formLogin().

    // ============================================================
    // FILTRO DE SEGURANÇA — DEFINE AS REGRAS DE ACESSO
    // ============================================================
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ----------------------------------------
                // REGRAS DE AUTORIZAÇÃO POR ROTA
                // ----------------------------------------
                .authorizeHttpRequests(auth -> auth

                        // Recursos estáticos — liberados para todos
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/style.css",
                                "/webjars/**"
                        ).permitAll()

                        // Tela de login — pública
                        .requestMatchers(LOGIN_PAGE, LOGIN_PAGE + "?error").permitAll()

                        // ----------------------------------------
                        // ROTAS EXCLUSIVAS DO SUPORTE
                        // ----------------------------------------
                        .requestMatchers(
                                "/dashboard/usuario/**",
                                "/dashboard/configuracoes/**"
                        ).hasRole(ROLE_SUPORTE)

                        // ----------------------------------------
                        // ROTAS DE CADASTRO — SUPORTE E PCP
                        // ----------------------------------------
                        .requestMatchers(
                                "/dashboard/setores/**",
                                "/dashboard/maquinas/**",
                                "/dashboard/produtos/**",
                                "/dashboard/roteiros/**"
                        ).hasAnyRole(ROLE_SUPORTE, ROLE_PCP_SUPERVISOR)

                        // ----------------------------------------
                        // CRIAÇÃO DE ORDENS — SUPORTE E PCP
                        // ----------------------------------------
                        .requestMatchers(
                                "/dashboard/ordens/nova",
                                "/dashboard/ordens/salvar"
                        ).hasAnyRole(ROLE_SUPORTE, ROLE_PCP_SUPERVISOR)

                        // ----------------------------------------
                        // VISUALIZAÇÃO DE ORDENS — TODOS
                        // ----------------------------------------
                        .requestMatchers("/dashboard/ordens/**").authenticated()

                        // ----------------------------------------
                        // LANÇAMENTOS — TODOS OS LOGADOS
                        // ----------------------------------------
                        .requestMatchers("/dashboard/lancamentos/**").authenticated()

                        // ----------------------------------------
                        // VIDRAÇARIA — TODOS OS LOGADOS
                        // ----------------------------------------
                        .requestMatchers("/dashboard/vidracaria/**").authenticated()

                        // ----------------------------------------
                        // RELATÓRIOS — SUPORTE, GESTÃO E PCP
                        // ----------------------------------------
                        .requestMatchers("/dashboard/relatorios/**")
                        .hasAnyRole(ROLE_SUPORTE, ROLE_GESTAO, ROLE_PCP_SUPERVISOR)

                        // ----------------------------------------
                        // DASHBOARD — TODOS OS LOGADOS (regra genérica,
                        // precisa vir por último, depois das específicas acima)
                        // ----------------------------------------
                        .requestMatchers("/dashboard/**").authenticated()

                        // Qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )

                // ----------------------------------------
                // CONFIGURAÇÃO DO FORMULÁRIO DE LOGIN
                // ----------------------------------------
                .formLogin(form -> form
                        .loginPage(LOGIN_PAGE)                     // tela de login personalizada
                        .loginProcessingUrl(LOGIN_PAGE)            // URL que processa o POST do formulário
                        .usernameParameter("login")                // nome do campo no HTML (th:field="*{login}")
                        .passwordParameter("senha")                // nome do campo no HTML (th:field="*{senha}")
                        .defaultSuccessUrl("/dashboard", true)     // redireciona após login bem-sucedido
                        .failureUrl(LOGIN_PAGE + "?error=true")    // redireciona em caso de erro
                        .permitAll()
                )

                // ----------------------------------------
                // CONFIGURAÇÃO DO LOGOUT
                // logoutUrl() substitui o antigo logoutRequestMatcher(new
                // AntPathRequestMatcher(...)), que está deprecated.
                // ----------------------------------------
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl(LOGIN_PAGE + "?logout=true") // redireciona após logout
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")                  // remove o cookie de sessão
                        .permitAll()
                )
                .csrf(Customizer.withDefaults());

                // ----------------------------------------
                // DESATIVA CSRF PARA HTMX (requisições parciais)
                // Se preferir manter CSRF, configure o token no HTML
                // ----------------------------------------;

        return http.build();
    }

    // ============================================================
    // BCRYPT — ENCODER DE SENHAS
    // Fator de custo padrão: 10 (bom equilíbrio segurança/velocidade)
    // ============================================================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ============================================================
    // AUTHENTICATION MANAGER
    // Necessário para autenticar manualmente em alguns casos
    // ============================================================
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}