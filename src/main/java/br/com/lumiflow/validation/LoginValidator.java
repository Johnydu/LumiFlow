package br.com.lumiflow.validation;

import java.util.regex.Pattern;

/**
 * Validador de login e senha.
 * Proteção contra entradas inválidas e padronização de credenciais.
 */
public final class LoginValidator {

    private LoginValidator() {
        throw new IllegalStateException("Classe utilitária");
    }

    /**
     * Login:
     * - 3 a 50 caracteres
     * - letras
     * - números
     * - ponto
     * - underscore
     * - hífen
     */
    private static final Pattern LOGIN_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._-]{3,50}$");

    /**
     * Senha:
     * - mínimo 8 caracteres
     * - 1 letra minúscula
     * - 1 letra maiúscula
     * - 1 número
     */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile(
                    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
            );

    /**
     * Valida login.
     */
    public static boolean isValidLogin(String login) {
        if (login == null || login.isBlank()) {
            return false;
        }
        return LOGIN_PATTERN.matcher(login).matches();
    }

    /**
     * Valida senha.
     */
    public static boolean isValidPassword(String password) {

        if (password == null) {
            return false;
        }

        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Mensagem para exibição ao usuário.
     */
    public static String getPasswordRequirements() {
        return """
                A senha deve conter:
                - mínimo de 8 caracteres
                - pelo menos 1 letra maiúscula
                - pelo menos 1 letra minúscula
                - pelo menos 1 número
                """;
    }
}