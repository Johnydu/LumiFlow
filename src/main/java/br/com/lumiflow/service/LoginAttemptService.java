package br.com.lumiflow.service;

import br.com.lumiflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Regra de negócio do bloqueio de login por conta.
 * Responsabilidade única: contar falhas e aplicar/remover o bloqueio temporário.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UsuarioRepository usuarioRepository;

    @Value("${lumiflow.security.login.max-tentativas:5}")
    private int maxTentativas;

    @Value("${lumiflow.security.login.bloqueio-minutos:15}")
    private long bloqueioMinutos;

    @Transactional
    public void registrarFalha(String login) {
        if (login == null || login.isBlank()) {
            return;
        }
        // Login inexistente: nada a contar (e nada a revelar ao chamador).
        if (usuarioRepository.incrementarTentativasFalhas(login) == 0) {
            return;
        }
        LocalDateTime ate = LocalDateTime.now().plusMinutes(bloqueioMinutos);
        if (usuarioRepository.bloquearSeExcedeuLimite(login, ate, maxTentativas) > 0) {
            log.warn("Login bloqueado por excesso de tentativas: login={}, ate={}", login, ate);
        }
    }

    @Transactional
    public void registrarSucesso(String login) {
        if (login != null && !login.isBlank()) {
            usuarioRepository.limparControleDeBloqueio(login);
        }
    }
}