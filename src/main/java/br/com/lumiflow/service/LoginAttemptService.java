package br.com.lumiflow.service;

import br.com.lumiflow.entity.enums.Descricao;
import br.com.lumiflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Regra de negócio do bloqueio de login por conta.
 * Responsabilidade única: contar falhas e aplicar/remover o bloqueio temporário.
 * <p>
 * Mitigação de DoS dirigido: o limite de tentativas continua curto (o terminal é compartilhado e o
 * operador legítimo precisa voltar logo), mas a duração do bloqueio escalona a cada bloqueio
 * consecutivo (padrão 1, 5 e 15 minutos). Um atacante que trava a conta de alguém só a mantém
 * indisponível por 1 minuto na primeira vez, e o nível volta a zero após um login bem-sucedido
 * ou após {@code lumiflow.security.login.reset-escalonamento-horas} sem novos bloqueios.
 * <p>
 * O nível de escalonamento fica em memória (não exige mudança de schema): ao reiniciar a
 * aplicação ele zera, o que apenas abranda o bloqueio, nunca o endurece.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UsuarioRepository usuarioRepository;

    @Value("${lumiflow.security.login.max-tentativas:5}")
    private int maxTentativas;

    /** Duração (minutos) de cada bloqueio consecutivo; o último valor se repete. */
    @Value("${lumiflow.security.login.bloqueio-escala-minutos:1,5,15}")
    private long[] escalaMinutos;

    @Value("${lumiflow.security.login.reset-escalonamento-horas:24}")
    private long resetEscalonamentoHoras;

    /** Último nível de bloqueio por login (apenas logins que existem no banco entram aqui). */
    private final ConcurrentMap<String, EstadoBloqueio> estados = new ConcurrentHashMap<>();

    private record EstadoBloqueio(int bloqueios, LocalDateTime ultimoBloqueio) {
    }

    @Transactional
    public void registrarFalha(String login) {
        if (login == null || login.isBlank()) {
            return;
        }
        // Login inexistente: nada a contar (e nada a revelar ao chamador).
        if (usuarioRepository.incrementarTentativasFalhas(login) == 0) {
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        int nivel = nivelAtual(login, agora);
        long minutos = minutosDoNivel(nivel);
        LocalDateTime ate = agora.plusMinutes(minutos);

        if (usuarioRepository.bloquearSeExcedeuLimite(login, ate, maxTentativas) > 0) {
            estados.put(login, new EstadoBloqueio(nivel + 1, agora));
            log.warn("Login bloqueado por excesso de tentativas: login={}, bloqueio={}min, ate={}",
                    login, minutos, ate);
            alertarSeAdmin(login, minutos);
        }
    }

    @Transactional
    public void registrarSucesso(String login) {
        if (login != null && !login.isBlank()) {
            usuarioRepository.limparControleDeBloqueio(login);
            estados.remove(login);
        }
    }

    private int nivelAtual(String login, LocalDateTime agora) {
        EstadoBloqueio estado = estados.get(login);
        if (estado == null) {
            return 0;
        }
        if (Duration.between(estado.ultimoBloqueio(), agora).toHours() >= resetEscalonamentoHoras) {
            estados.remove(login);
            return 0;
        }
        return estado.bloqueios();
    }

    private long minutosDoNivel(int nivel) {
        if (escalaMinutos == null || escalaMinutos.length == 0) {
            return 15L;
        }
        return escalaMinutos[Math.min(nivel, escalaMinutos.length - 1)];
    }

    /** Conta ADMIN bloqueada é o alvo mais provável de um ataque dirigido: merece destaque no log. */
    private void alertarSeAdmin(String login, long minutos) {
        usuarioRepository.findByLogin(login)
                .filter(u -> u.getNivelAcesso() != null && u.getNivelAcesso().getDescricao() == Descricao.ADMIN)
                .ifPresent(u -> log.warn("ALERTA DE SEGURANCA: conta ADMIN bloqueada por tentativas de login "
                        + "falhas: login={}, bloqueio={}min", login, minutos));
    }
}
