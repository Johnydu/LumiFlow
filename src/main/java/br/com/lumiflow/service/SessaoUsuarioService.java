package br.com.lumiflow.service;

import br.com.lumiflow.security.UsuarioDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

/**
 * Encerra as sessões ativas de um usuário. Usado quando o usuário é excluído ou tem
 * login, perfil, setor ou senha alterados: sem isso, as permissões antigas continuariam
 * valendo até a sessão expirar sozinha.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessaoUsuarioService {

    private final SessionRegistry sessionRegistry;

    public void encerrarSessoes(String login) {
        if (login == null || login.isBlank()) {
            return;
        }
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (principal instanceof UsuarioDetails usuario && login.equals(usuario.getUsername())) {
                sessionRegistry.getAllSessions(principal, false).forEach(SessionInformation::expireNow);
                log.info("Sessões encerradas por alteração de cadastro: login={}", login);
            }
        }
    }
}
