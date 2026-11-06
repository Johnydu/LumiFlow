package br.com.lumiflow.service;

import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.security.UsuarioDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessaoUsuarioServiceTest {

    @Mock
    private SessionRegistry sessionRegistry;

    @InjectMocks
    private SessaoUsuarioService service;

    private UsuarioDetails detalhes(String login) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Fulano de Tal");
        usuario.setLogin(login);
        usuario.setSenha("hash");
        return new UsuarioDetails(usuario);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Login nulo, vazio ou em branco não consulta o registro de sessões")
    void ignoraLoginInvalido(String login) {
        service.encerrarSessoes(login);

        verifyNoInteractions(sessionRegistry);
    }

    @Test
    @DisplayName("Expira todas as sessões do usuário com o login informado")
    void expiraTodasAsSessoesDoUsuario() {
        UsuarioDetails alvo = detalhes("ana");
        SessionInformation s1 = org.mockito.Mockito.mock(SessionInformation.class);
        SessionInformation s2 = org.mockito.Mockito.mock(SessionInformation.class);
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(alvo));
        when(sessionRegistry.getAllSessions(alvo, false)).thenReturn(List.of(s1, s2));

        service.encerrarSessoes("ana");

        verify(s1).expireNow();
        verify(s2).expireNow();
    }

    @Test
    @DisplayName("Não encerra sessões de outros usuários")
    void naoAfetaOutrosUsuarios() {
        UsuarioDetails outro = detalhes("bruno");
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(outro));

        service.encerrarSessoes("ana");

        verify(sessionRegistry, never()).getAllSessions(outro, false);
    }

    @Test
    @DisplayName("Ignora principais que não são UsuarioDetails")
    void ignoraPrincipaisDeOutroTipo() {
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of("ana", 42));

        service.encerrarSessoes("ana");

        verify(sessionRegistry, never()).getAllSessions(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    @DisplayName("Comparação de login diferencia maiúsculas de minúsculas")
    void loginEhCaseSensitive() {
        UsuarioDetails alvo = detalhes("Ana");
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(alvo));

        service.encerrarSessoes("ana");

        verify(sessionRegistry, never()).getAllSessions(alvo, false);
    }

    @Test
    @DisplayName("Sem principais registrados nada é expirado")
    void semPrincipais() {
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of());

        service.encerrarSessoes("ana");

        verify(sessionRegistry).getAllPrincipals();
        verify(sessionRegistry, never()).getAllSessions(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
