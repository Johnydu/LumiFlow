package br.com.lumiflow.exception;

/**
 * Exceção base para erros de negócio.
 * Armazena a chave de mensagem (não a mensagem em si) para permitir
 * que o tratador de exceções resolva a mensagem via MessageSource.
 */
public class BusinessException extends RuntimeException {

    private final String messageKey;
    private final Object[] parameters;

    public BusinessException(String messageKey) {
        this(messageKey, null);
    }

    public BusinessException(String messageKey, Object[] parameters) {
        super(messageKey);
        this.messageKey = messageKey;
        this.parameters = parameters;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getParameters() {
        return parameters;
    }
}