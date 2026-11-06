package br.com.lumiflow.controller;

import br.com.lumiflow.config.MessageSourceConfig;
import br.com.lumiflow.exception.GlobalExceptionHandler;
import br.com.lumiflow.security.CustomAuthenticationSuccessHandler;
import br.com.lumiflow.security.LoginIpThrottle;
import br.com.lumiflow.security.SecurityConfig;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;
import java.util.Map;

/**
 * Configuração compartilhada dos testes de slice web dos controllers.
 * <p>
 * Não usa a classe LumiflowApplication (que carrega @EnableJpaAuditing e quebraria o slice sem JPA).
 * Marcada com @TestComponent para não ser capturada pelo component scan do contexto completo.
 * As views são substituídas por um stub que não renderiza Thymeleaf: os testes verificam
 * nome da view, redirects e atributos do model.
 */
@Configuration
@TestComponent
@Import({SecurityConfig.class, MessageSourceConfig.class, GlobalControllerAdvice.class, GlobalExceptionHandler.class})
public class ControllerSliceConfig {

    @Bean
    CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler() {
        return new CustomAuthenticationSuccessHandler();
    }

    @Bean
    LoginIpThrottle loginIpThrottle() {
        return new LoginIpThrottle(30, 15L);
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    /** O nome "viewResolver" impede o ContentNegotiatingViewResolver do Boot de ser criado. */
    @Bean(name = "viewResolver")
    ViewResolver viewResolver() {
        return new ViewResolverSemRenderizacao();
    }

    static class ViewResolverSemRenderizacao implements ViewResolver, Ordered {

        @Override
        public View resolveViewName(String viewName, Locale locale) {
            if (viewName.startsWith("redirect:") || viewName.startsWith("forward:")) {
                return null; // deixa os resolvers padrão criarem o RedirectView
            }
            return new View() {
                @Override
                public String getContentType() {
                    return "text/html;charset=UTF-8";
                }

                @Override
                public void render(Map<String, ?> model, jakarta.servlet.http.HttpServletRequest request,
                                   jakarta.servlet.http.HttpServletResponse response) {
                    // sem renderização
                }
            };
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }
}
