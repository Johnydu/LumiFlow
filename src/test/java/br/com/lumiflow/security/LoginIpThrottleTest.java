package br.com.lumiflow.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoginIpThrottle")
class LoginIpThrottleTest {

    @Test
    @DisplayName("IP sem falhas nao esta bloqueado")
    void semFalhas() {
        assertThat(new LoginIpThrottle(3, 15).estaBloqueado("1.1.1.1")).isFalse();
    }

    @Test
    @DisplayName("abaixo do limite nao bloqueia")
    void abaixoDoLimite() {
        LoginIpThrottle throttle = new LoginIpThrottle(3, 15);
        throttle.registrarFalha("1.1.1.1");
        throttle.registrarFalha("1.1.1.1");

        assertThat(throttle.estaBloqueado("1.1.1.1")).isFalse();
    }

    @Test
    @DisplayName("ao atingir exatamente o limite bloqueia e continua bloqueado em novas falhas")
    void noLimiteBloqueia() {
        LoginIpThrottle throttle = new LoginIpThrottle(3, 15);
        for (int i = 0; i < 3; i++) {
            throttle.registrarFalha("1.1.1.1");
        }
        assertThat(throttle.estaBloqueado("1.1.1.1")).isTrue();

        throttle.registrarFalha("1.1.1.1");
        assertThat(throttle.estaBloqueado("1.1.1.1")).isTrue();
    }

    @Test
    @DisplayName("bloqueio de um IP nao afeta outros IPs")
    void isolamentoPorIp() {
        LoginIpThrottle throttle = new LoginIpThrottle(2, 15);
        throttle.registrarFalha("1.1.1.1");
        throttle.registrarFalha("1.1.1.1");

        assertThat(throttle.estaBloqueado("1.1.1.1")).isTrue();
        assertThat(throttle.estaBloqueado("2.2.2.2")).isFalse();
    }

    @Test
    @DisplayName("limite 1 bloqueia na primeira falha")
    void limiteUm() {
        LoginIpThrottle throttle = new LoginIpThrottle(1, 15);
        throttle.registrarFalha("9.9.9.9");

        assertThat(throttle.estaBloqueado("9.9.9.9")).isTrue();
    }

    @Test
    @DisplayName("brute force concorrente: nenhuma falha e perdida e o IP termina bloqueado")
    void bruteForceConcorrente() throws Exception {
        int tentativas = 200;
        LoginIpThrottle throttle = new LoginIpThrottle(tentativas, 15);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch fim = new CountDownLatch(tentativas);
        try {
            for (int i = 0; i < tentativas; i++) {
                pool.submit(() -> {
                    try {
                        largada.await();
                        throttle.registrarFalha("6.6.6.6");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        fim.countDown();
                    }
                });
            }
            largada.countDown();
            assertThat(fim.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        // Se alguma contagem tivesse sido perdida, o total ficaria abaixo do limite.
        assertThat(throttle.estaBloqueado("6.6.6.6")).isTrue();
    }

    @Test
    @DisplayName("muitos IPs forjados distintos nao bloqueiam nenhum deles individualmente")
    void ipsForjadosDistintos() {
        LoginIpThrottle throttle = new LoginIpThrottle(5, 15);
        for (int i = 0; i < 500; i++) {
            throttle.registrarFalha("10.0." + (i / 250) + "." + (i % 250));
        }

        assertThat(throttle.estaBloqueado("10.0.0.1")).isFalse();
    }
}
