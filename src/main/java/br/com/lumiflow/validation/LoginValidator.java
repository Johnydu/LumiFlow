package br.com.lumiflow.validation;

import java.util.regex.Pattern;

/**
 * Validador de entrada para login - Previne SQL Injection e XSS
 */
public class LoginValidator {

    // Padrão: apenas letras, números, underscores e hífens (3-50 caracteres)
    private static final Pattern LOGIN_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");
    
    // Padrão de senha forte: min 12 chars, 1 maiúscula, 1 minúscula, 1 número, 1 especial
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$");

    /**
     * Valida se o login segue o padrão de segurança
     */
    public static boolean isValidLogin(String login) {
        if (login == null || login.isBlank()) {
            return false;
        }
        return LOGIN_PATTERN.matcher(login.trim()).matches();
    }

    /**
     * Valida se a senha atende aos requisitos de força
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 12) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Mensagem de erro para senha fraca
     */
    public static String getPasswordRequirements() {
        return "Senha deve conter: mínimo 12 caracteres, 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial (@$!%*?&)";
    }
}
