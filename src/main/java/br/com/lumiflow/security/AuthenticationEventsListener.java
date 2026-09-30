package br.com.lumiflow.security;

import br.com.lumiflow.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

/**
 * Reage aos eventos de autenticação publicados pelo Spring Security.
 * Mantém o SecurityConfig limpo e a contagem desacoplada do fluxo de login.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationEventsListener {

    private final LoginAttemptService loginAttemptService;
    private final LoginIpThrottle ipThrottle;

    @EventListener
    public void aoFalharCredenciais(AuthenticationFailureBadCredentialsEvent event) {
        String login = event.getAuthentication().getName();
        String ip = extrairIp(event.getAuthentication());

        log.warn("Falha de login: login={}, ip={}", sanitizar(login), ip);
        ipThrottle.registrarFalha(ip);
        loginAttemptService.registrarFalha(login);
    }

    @EventListener
    public void aoTentarContaBloqueada(AuthenticationFailureLockedEvent event) {
        String ip = extrairIp(event.getAuthentication());

        log.warn("Tentativa em conta bloqueada: login={}, ip={}",
                sanitizar(event.getAuthentication().getName()), ip);
        ipThrottle.registrarFalha(ip);
    }

    @EventListener
    public void aoAutenticarComSucesso(AuthenticationSuccessEvent event) {
        loginAttemptService.registrarSucesso(event.getAuthentication().getName());
    }

    private String extrairIp(Authentication authentication) {
        Object detalhes = authentication.getDetails();
        return detalhes instanceof WebAuthenticationDetails web ? web.getRemoteAddress() : "desconhecido";
    }

    /** Evita log injection (quebras de linha) e valores gigantes vindos do formulário. */
    private String sanitizar(String valor) {
        if (valor == null) {
            return "";
        }
        String limpo = valor.replaceAll("[\\r\\n\\t]", "_");
        return limpo.length() > 50 ? limpo.substring(0, 50) : limpo;
    }
}