package br.com.lumiflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Barra o POST /login de IPs que excederam o limite de falhas, antes de
 * qualquer consulta ao banco ou verificação de senha.
 *
 * Não é um @Component de propósito: é registrado apenas na cadeia do
 * Spring Security (SecurityConfig), evitando execução em duplicidade.
 */
@Slf4j
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginIpThrottle throttle;

    public LoginRateLimitFilter(LoginIpThrottle throttle) {
        this.throttle = throttle;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(caminhoSemContexto(request)));
    }

    /**
     * URI da requisição sem o context path, sem parâmetros de caminho (";jsessionid=...") e sem barras finais,
     * para que "/app/login", "/login/" e "/login;x=1" sejam tratados como o login.
     */
    static String caminhoSemContexto(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        String contexto = request.getContextPath();
        if (contexto != null && !contexto.isEmpty() && caminho.startsWith(contexto)) {
            caminho = caminho.substring(contexto.length());
        }
        int parametros = caminho.indexOf(';');
        if (parametros >= 0) {
            caminho = caminho.substring(0, parametros);
        }
        while (caminho.length() > 1 && caminho.endsWith("/")) {
            caminho = caminho.substring(0, caminho.length() - 1);
        }
        return caminho;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        if (throttle.estaBloqueado(ip)) {
            log.warn("Tentativa de login barrada por limite de IP: ip={}", ip);
            // Mesma tela e mesma mensagem genérica de qualquer falha de login.
            response.sendRedirect(request.getContextPath() + "/login?error=true");
            return;
        }
        chain.doFilter(request, response);
    }
}