package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.exception.BusinessException;
import org.springframework.context.MessageSource;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.Locale;

/**
 * Resolução centralizada das mensagens exibidas pela camada web.
 * Evita repetir {@code messageSource.getMessage(...)} (e esquecer os argumentos da exceção)
 * em cada controller.
 */
public final class MensagensWeb {

    private MensagensWeb() {
    }

    /** Mensagem da BusinessException com os argumentos aplicados; sem tradução, devolve a própria chave. */
    public static String de(MessageSource messageSource, BusinessException e, Locale locale) {
        return messageSource.getMessage(e.getMessageKey(), e.getArgs(), e.getMessageKey(), locale);
    }

    /** Mensagem genérica de dados inválidos (usada também para falhas inesperadas nos controllers). */
    public static String validacaoFalhou(MessageSource messageSource, Locale locale) {
        return messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale);
    }

    /**
     * Mensagem para o primeiro erro de validação. Resolve a chave i18n das anotações de validação (ou o texto
     * da própria anotação). Erros de binding/conversão (texto técnico do Spring, ex.:
     * "Failed to convert property value...") viram a mensagem genérica.
     */
    public static String deBinding(MessageSource messageSource, BindingResult result, Locale locale) {
        ObjectError primeiro = result.getAllErrors().stream().findFirst().orElse(null);
        if (primeiro == null) {
            return validacaoFalhou(messageSource, locale);
        }
        boolean falhaDeBinding = primeiro instanceof FieldError campo && campo.isBindingFailure();
        String chave = primeiro.getDefaultMessage();
        if (falhaDeBinding || chave == null || chave.isBlank()) {
            return validacaoFalhou(messageSource, locale);
        }
        return messageSource.getMessage(chave, null, chave, locale);
    }
}
