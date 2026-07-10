package br.com.lumiflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // permite usar @PreAuthorize nos controllers
public class SecurityConfig {

    // ============================================================
    // INJEÇÃO DE DEPENDÊNCIA VIA CONSTRUTOR (SOLID - DIP)
    // ============================================================
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

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
                        .requestMatchers("/login", "/login?error").permitAll()

                        // ----------------------------------------
                        // ROTAS EXCLUSIVAS DO SUPORTE
                        // ----------------------------------------
                        .requestMatchers(
                                "/usuarios/**",
                                "/configuracoes/**"
                        ).hasRole("SUPORTE")

                        // ----------------------------------------
                        // ROTAS DE CADASTRO — SUPORTE E PCP
                        // ----------------------------------------
                        .requestMatchers(
                                "/setores/**",
                                "/maquinas/**",
                                "/produtos/**",
                                "/roteiros/**"
                        ).hasAnyRole("SUPORTE", "PCP_SUPERVISOR")

                        // ----------------------------------------
                        // CRIAÇÃO DE ORDENS — SUPORTE E PCP
                        // ----------------------------------------
                        .requestMatchers(
                                "/ordens/nova",
                                "/ordens/salvar"
                        ).hasAnyRole("SUPORTE", "PCP_SUPERVISOR")

                        // ----------------------------------------
                        // VISUALIZAÇÃO DE ORDENS — TODOS
                        // ----------------------------------------
                        .requestMatchers("/ordens/**").authenticated()

                        // ----------------------------------------
                        // LANÇAMENTOS — TODOS OS LOGADOS
                        // ----------------------------------------
                        .requestMatchers("/lancamentos/**").authenticated()

                        // ----------------------------------------
                        // VIDRAÇARIA — TODOS OS LOGADOS
                        // ----------------------------------------
                        .requestMatchers("/vidracaria/**").authenticated()

                        // ----------------------------------------
                        // RELATÓRIOS — SUPORTE, GESTÃO E PCP
                        // ----------------------------------------
                        .requestMatchers("/relatorios/**")
                        .hasAnyRole("SUPORTE", "GESTAO", "PCP_SUPERVISOR")

                        // ----------------------------------------
                        // DASHBOARD — TODOS OS LOGADOS
                        // ----------------------------------------
                        .requestMatchers("/dashboard/**").authenticated()

                        // Qualquer outra rota exige autenticação
                        .anyRequest().authenticated()
                )

                // ----------------------------------------
                // CONFIGURAÇÃO DO FORMULÁRIO DE LOGIN
                // ----------------------------------------
                .formLogin(form -> form
                        .loginPage("/login")               // tela de login personalizada
                        .loginProcessingUrl("/login")      // URL que processa o POST do formulário
                        .usernameParameter("login")        // nome do campo no HTML (th:field="*{login}")
                        .passwordParameter("senha")        // nome do campo no HTML (th:field="*{senha}")
                        .defaultSuccessUrl("/dashboard", true) // redireciona após login bem-sucedido
                        .failureUrl("/login?error=true")   // redireciona em caso de erro
                        .permitAll()
                )

                // ----------------------------------------
                // CONFIGURAÇÃO DO LOGOUT
                // ----------------------------------------
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout=true") // redireciona após logout
                        .invalidateHttpSession(true)            // invalida a sessão
                        .deleteCookies("JSESSIONID")            // remove o cookie de sessão
                        .permitAll()
                )

                // ----------------------------------------
                // DESATIVA CSRF PARA HTMX (requisições parciais)
                // Se preferir manter CSRF, configure o token no HTML
                // ----------------------------------------
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/dashboard/**", "/ordens/**", "/lancamentos/**")
                );

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
