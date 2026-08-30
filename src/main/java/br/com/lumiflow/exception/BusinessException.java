package br.com.lumiflow.exception;

/**
 * Exceção genérica para qualquer violação de regra de negócio.
 * Carrega uma CHAVE de mensagem (não o texto pronto) + argumentos
 * para o Spring resolver via messages.properties na camada de apresentação.
 */
public class BusinessException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    public BusinessException(String messageKey, Object... args) {
        // super(messageKey) mantém uma mensagem "crua" pra logs técnicos,
        // mesmo antes de resolver o texto amigável
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args;
    }
}