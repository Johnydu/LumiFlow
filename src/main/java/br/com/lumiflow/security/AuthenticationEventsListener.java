package br.com.lumiflow.security;

import br.com.lumiflow.service.LoginAttemptService;
import br.com.lumiflow.validation.LoginValidator;
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

    private static final String LOGIN_MASCARADO = "[login-invalido]";

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

    /**
     * Só registra o texto digitado quando ele tem formato de login válido (3-20 caracteres seguros).
     * Qualquer outra coisa (senha digitada no campo errado, lixo, tentativa de injeção) é mascarada,
     * o que também elimina log injection e valores gigantes vindos do formulário.
     */
    private String sanitizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return "";
        }
        return LoginValidator.isValidLogin(valor) ? valor : LOGIN_MASCARADO;
    }
}