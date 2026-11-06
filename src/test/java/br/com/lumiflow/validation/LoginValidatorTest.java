package br.com.lumiflow.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LoginValidator")
class LoginValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"abc", "Joao.Silva", "user_01", "a-b-c", "ABC123", "a.b_c-d", "12345678901234567890"})
    @DisplayName("logins validos")
    void loginsValidos(String login) {
        assertThat(LoginValidator.isValidLogin(login)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "ab", "123456789012345678901", "joao silva", "joao@empresa", "joão",
            "admin'--", "<script>", "a/b", "abc\n", "abc\r\n", "\nabc", "abc\u0000", "..;/", "ab c"})
    @DisplayName("logins invalidos, brancos, injecao e quebras de linha")
    void loginsInvalidos(String login) {
        assertThat(LoginValidator.isValidLogin(login)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abcdef12", "SenhaForte1", "aA1aaaaa", "Xyz12345!@#", "Senha123Segura"})
    @DisplayName("senhas validas")
    void senhasValidas(String senha) {
        assertThat(LoginValidator.isValidPassword(senha)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Abc1234", "abcdefg1", "ABCDEFG1", "Abcdefgh", "12345678", "        ", "senhafraca",
            "Abcdefg٣"})
    @DisplayName("senhas invalidas: curta, sem maiuscula/minuscula/digito ASCII")
    void senhasInvalidas(String senha) {
        assertThat(LoginValidator.isValidPassword(senha)).isFalse();
    }

    @Test
    @DisplayName("limite do bcrypt: 72 bytes e aceito, 73 bytes e rejeitado")
    void limiteBcrypt() {
        String com72 = "Aa1" + "x".repeat(69);
        String com73 = "Aa1" + "x".repeat(70);

        assertThat(com72.length()).isEqualTo(72);
        assertThat(LoginValidator.isValidPassword(com72)).isTrue();
        assertThat(LoginValidator.isValidPassword(com73)).isFalse();
    }

    @Test
    @DisplayName("limite e medido em bytes UTF-8, nao em caracteres")
    void limiteEmBytes() {
        String com71Bytes = "Aa1" + "é".repeat(34); // 3 + 68 = 71 bytes
        String com73Bytes = "Aa1" + "é".repeat(35); // 3 + 70 = 73 bytes

        assertThat(LoginValidator.isValidPassword(com71Bytes)).isTrue();
        assertThat(LoginValidator.isValidPassword(com73Bytes)).isFalse();
    }

    @Test
    @DisplayName("senha enorme (DoS no bcrypt/regex) e rejeitada rapidamente")
    void senhaEnorme() {
        String gigante = "Aa1" + "x".repeat(1_000_000);

        assertThat(LoginValidator.isValidPassword(gigante)).isFalse();
    }

    @Test
    @DisplayName("minimo de 8 bytes: 7 caracteres rejeitado, 8 aceito")
    void limiteMinimo() {
        assertThat(LoginValidator.isValidPassword("Aa1xxxx")).isFalse();
        assertThat(LoginValidator.isValidPassword("Aa1xxxxx")).isTrue();
    }

    @Test
    @DisplayName("requisitos de senha devem listar as regras principais")
    void mensagemRequisitos() {
        assertThat(LoginValidator.getPasswordRequirements())
                .contains("8 caracteres")
                .contains("maiúscula")
                .contains("minúscula")
                .contains("número");
    }

    @Test
    @DisplayName("classe utilitaria nao pode ser instanciada")
    void naoInstanciavel() throws Exception {
        Constructor<LoginValidator> ctor = LoginValidator.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        assertThatThrownBy(ctor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
