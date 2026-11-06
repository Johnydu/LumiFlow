package br.com.lumiflow.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    @DisplayName("Deve guardar a chave da mensagem e usá-la como mensagem técnica")
    void deveGuardarChaveDaMensagem() {
        BusinessException ex = new BusinessException("error.user.notfound");

        assertThat(ex.getMessageKey()).isEqualTo("error.user.notfound");
        assertThat(ex.getMessage()).isEqualTo("error.user.notfound");
        assertThat(ex.getArgs()).isEmpty();
    }

    @Test
    @DisplayName("Deve guardar os argumentos informados na ordem")
    void deveGuardarArgumentos() {
        BusinessException ex = new BusinessException("error.x", 10L, "abc");

        assertThat(ex.getArgs()).containsExactly(10L, "abc");
    }

    @Test
    @DisplayName("Deve ser uma RuntimeException (não verificada)")
    void deveSerRuntimeException() {
        assertThat(new BusinessException("k")).isInstanceOf(RuntimeException.class);
    }
}
