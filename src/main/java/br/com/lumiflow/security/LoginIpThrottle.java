package br.com.lumiflow.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite de falhas de login por IP (janela fixa, em memória).
 * Complementa o bloqueio por conta: protege contra "password spraying"
 * (uma senha testada contra muitos usuários).
 */
@Component
public class LoginIpThrottle {

    private final Cache<String, AtomicInteger> falhasPorIp;
    private final int maxFalhas;

    public LoginIpThrottle(
            @Value("${lumiflow.security.login.ip-max-falhas:30}") int maxFalhas,
            @Value("${lumiflow.security.login.ip-janela-minutos:15}") long janelaMinutos) {
        this.maxFalhas = maxFalhas;
        this.falhasPorIp = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(janelaMinutos)) // janela fixa a partir da 1ª falha
                .maximumSize(10_000)                                 // evita esgotar memória com IPs forjados
                .build();
    }

    public boolean estaBloqueado(String ip) {
        AtomicInteger falhas = falhasPorIp.getIfPresent(ip);
        return falhas != null && falhas.get() >= maxFalhas;
    }

    public void registrarFalha(String ip) {
        falhasPorIp.get(ip, chave -> new AtomicInteger()).incrementAndGet();
    }
}