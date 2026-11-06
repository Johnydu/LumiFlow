package br.com.lumiflow.security;

import br.com.lumiflow.service.LoginAttemptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationEventsListener")
class AuthenticationEventsListenerTest {

    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private LoginIpThrottle ipThrottle;
    @InjectMocks
    private AuthenticationEventsListener listener;

    private Authentication auth(String login, String ip) {
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(login, "x");
        if (ip != null) {
            token.setDetails(new WebAuthenticationDetails(ip, "sessao-1"));
        }
        return token;
    }

    @Test
    @DisplayName("falha de credenciais deve registrar no throttle de IP e na conta")
    void falhaCredenciais() {
        listener.aoFalharCredenciais(
                new AuthenticationFailureBadCredentialsEvent(auth("joao", "10.1.1.1"), new BadCredentialsException("x")));

        verify(ipThrottle).registrarFalha("10.1.1.1");
        verify(loginAttemptService).registrarFalha("joao");
    }

    @Test
    @DisplayName("falha sem WebAuthenticationDetails deve usar IP 'desconhecido'")
    void falhaSemDetalhes() {
        listener.aoFalharCredenciais(
                new AuthenticationFailureBadCredentialsEvent(auth("joao", null), new BadCredentialsException("x")));

        verify(ipThrottle).registrarFalha("desconhecido");
        verify(loginAttemptService).registrarFalha("joao");
    }

    @Test
    @DisplayName("tentativa em conta bloqueada conta no IP mas nao incrementa a conta")
    void contaBloqueada() {
        listener.aoTentarContaBloqueada(
                new AuthenticationFailureLockedEvent(auth("joao", "10.2.2.2"), new LockedException("x")));

        verify(ipThrottle).registrarFalha("10.2.2.2");
        verifyNoInteractions(loginAttemptService);
    }

    @Test
    @DisplayName("sucesso deve limpar controle de bloqueio da conta sem tocar no throttle de IP")
    void sucesso() {
        listener.aoAutenticarComSucesso(new AuthenticationSuccessEvent(auth("joao", "10.3.3.3")));

        verify(loginAttemptService).registrarSucesso("joao");
        verifyNoMoreInteractions(loginAttemptService);
        verifyNoInteractions(ipThrottle);
    }

    @Test
    @DisplayName("login com quebras de linha e gigante (log injection) nao deve quebrar o listener")
    void logInjection() {
        String malicioso = "admin\r\n2025-01-01 INFO FORJADO\t" + "A".repeat(5000);

        assertThatCode(() -> listener.aoFalharCredenciais(
                new AuthenticationFailureBadCredentialsEvent(auth(malicioso, "10.4.4.4"), new BadCredentialsException("x"))))
                .doesNotThrowAnyException();

        // A sanitizacao vale so para o log: o valor bruto segue para o servico (que consulta por login exato).
        verify(loginAttemptService).registrarFalha(malicioso);
        verify(ipThrottle).registrarFalha("10.4.4.4");
    }

    @Test
    @DisplayName("login nulo (principal nulo no nome) nao deve lancar excecao no evento de bloqueio")
    void loginNuloBloqueado() {
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated("", "x");
        token.setDetails(new WebAuthenticationDetails("10.5.5.5", null));

        assertThatCode(() -> listener.aoTentarContaBloqueada(
                new AuthenticationFailureLockedEvent(token, new LockedException("x"))))
                .doesNotThrowAnyException();
        verify(ipThrottle).registrarFalha("10.5.5.5");
    }
}
