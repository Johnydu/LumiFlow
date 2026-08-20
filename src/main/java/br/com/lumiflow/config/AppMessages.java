package br.com.lumiflow.config;

/**
 * Centraliza as mensagens exibidas pela aplicação para manter consistência,
 * facilitar a manutenção e preparar o projeto para futuras traduções.
 */
public final class AppMessages {

    private AppMessages() {
    }

    // SUCCESS
    public static final String SUCCESS_USER_CREATED = "Usuário cadastrado com sucesso.";
    public static final String SUCCESS_USER_UPDATED = "Usuário atualizado com sucesso.";
    public static final String SUCCESS_USER_DELETED = "Usuário excluído com sucesso.";
    public static final String SUCCESS_LOGIN = "Login realizado com sucesso.";
    public static final String SUCCESS_LOGOUT = "Logout realizado com sucesso.";
    public static final String SUCCESS_MACHINE_CREATED = "Máquina cadastrada com sucesso.";
    public static final String SUCCESS_MACHINE_UPDATED = "Máquina atualizada com sucesso.";
    public static final String SUCCESS_MACHINE_DELETED = "Máquina removida com sucesso.";
    public static final String SUCCESS_OPERATOR_CREATED = "Operador cadastrado com sucesso.";
    public static final String SUCCESS_OPERATOR_UPDATED = "Operador atualizado com sucesso.";
    public static final String SUCCESS_OPERATOR_DELETED = "Operador excluído com sucesso.";
    public static final String SUCCESS_PRODUCT_CREATED = "Produto cadastrado com sucesso.";
    public static final String SUCCESS_PRODUCT_UPDATED = "Produto atualizado com sucesso.";
    public static final String SUCCESS_PRODUCT_DELETED = "Produto excluído com sucesso.";
    public static final String SUCCESS_SECTOR_CREATED = "Setor cadastrado com sucesso.";
    public static final String SUCCESS_SECTOR_UPDATED = "Setor atualizado com sucesso.";
    public static final String SUCCESS_SECTOR_DELETED = "Setor removido com sucesso.";
    public static final String SUCCESS_ROUTE_CREATED = "Roteiro cadastrado com sucesso.";
    public static final String SUCCESS_ROUTE_UPDATED = "Roteiro atualizado com sucesso.";
    public static final String SUCCESS_ROUTE_DELETED = "Roteiro removido com sucesso.";
    public static final String SUCCESS_GLASS_MOVEMENT_CREATED = "Movimentação registrada com sucesso.";
    public static final String SUCCESS_GLASS_TYPE_CREATED = "Tipo de vidro cadastrado com sucesso.";

    // ERROR
    public static final String ERROR_USER_NOT_FOUND = "Usuário não encontrado.";
    public static final String ERROR_USER_ALREADY_EXISTS = "Já existe um usuário com estes dados.";
    public static final String ERROR_USER_INVALID_CREDENTIALS = "Usuário ou senha inválidos.";
    public static final String ERROR_USER_INACTIVE = "Usuário inativo.";
    public static final String ERROR_USER_PERMISSION_DENIED = "Você não possui permissão para esta ação.";
    public static final String ERROR_MACHINE_NOT_FOUND = "Máquina não encontrada.";
    public static final String ERROR_INTERNAL_SERVER = "Ocorreu um erro inesperado. A equipe técnica foi notificada.";
    public static final String ERROR_UNAUTHORIZED = "Acesso não autorizado.";
    public static final String ERROR_FORBIDDEN = "Acesso proibido.";
    public static final String ERROR_VALIDATION_FAILED = "Preencha os campos corretamente.";
    public static final String ERROR_DATABASE_CONNECTION = "Não foi possível conectar ao banco de dados.";

    // INFO
    public static final String INFO_NO_RECORDS = "Nenhum registro encontrado.";
    public static final String INFO_CONFIRM_DELETE = "Confirma a exclusão deste registro?";
    public static final String INFO_SESSION_EXPIRED = "Sua sessão expirou. Faça login novamente.";
}
