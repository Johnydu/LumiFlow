package br.com.lumiflow.config;

/**
 * Centralized keys for all user-facing application messages.
 */
public final class AppMessages {
    private AppMessages() {
    }

    public static final String SUCCESS_USER_CREATED = "success.user.created";
    public static final String SUCCESS_USER_UPDATED = "success.user.updated";
    public static final String SUCCESS_USER_DELETED = "success.user.deleted";
    public static final String SUCCESS_USER_LOGIN = "success.user.login";
    public static final String SUCCESS_USER_LOGOUT = "success.user.logout";
    public static final String SUCCESS_PRODUCT_CREATED = "success.produto.created";
    public static final String SUCCESS_PRODUCT_UPDATED = "success.produto.updated";
    public static final String SUCCESS_PRODUCT_DELETED = "success.produto.deleted";
    public static final String SUCCESS_MACHINE_CREATED = "success.machine.created";
    public static final String SUCCESS_MACHINE_UPDATED = "success.machine.updated";
    public static final String SUCCESS_MACHINE_DELETED = "success.machine.deleted";
    public static final String SUCCESS_OPERATOR_CREATED = "success.operador.created";
    public static final String SUCCESS_OPERATOR_UPDATED = "success.operador.updated";
    public static final String SUCCESS_OPERATOR_DELETED = "success.operador.deleted";
    public static final String SUCCESS_SECTOR_CREATED = "success.sector.created";
    public static final String SUCCESS_SECTOR_UPDATED = "success.sector.updated";
    public static final String SUCCESS_SECTOR_DELETED = "success.sector.deleted";
    public static final String SUCCESS_ROUTE_CREATED = "success.route.created";
    public static final String SUCCESS_ROUTE_UPDATED = "success.route.updated";
    public static final String SUCCESS_ROUTE_DELETED = "success.route.deleted";
    public static final String SUCCESS_GLASS_MOVEMENT_CREATED = "success.glass.movement.created";
    public static final String SUCCESS_GLASS_TYPE_CREATED = "success.glass.type.created";
    public static final String ERROR_USER_NOTFOUND = "error.user.notfound";
    public static final String ERROR_USER_ALREADY_EXISTS = "error.user.already.exists";
    public static final String ERROR_USER_LOGIN_INVALID = "error.user.login.invalid";
    public static final String ERROR_USER_PASSWORD_WEAK = "error.user.password.weak";
    public static final String ERROR_USER_INVALID_CREDENTIALS = "error.user.invalid.credentials";
    public static final String ERROR_PRODUCT_NOTFOUND = "error.produto.notfound";
    public static final String ERROR_PRODUCT_CODIGO_DUPLICATE = "error.produto.codigo.duplicate";
    public static final String ERROR_PRODUCT_NOME_DUPLICATE = "error.produto.nome.duplicate";
    public static final String ERROR_MACHINE_NOTFOUND = "error.machine.notfound";
    public static final String ERROR_MACHINE_NAME_DUPLICATE = "error.machine.name.duplicate";
    public static final String ERROR_OPERATOR_NOTFOUND = "error.operator.notfound";
    public static final String ERROR_OPERATOR_NAME_DUPLICATE = "error.operator.name.duplicate";
    public static final String ERROR_SECTOR_NOTFOUND = "error.sector.notfound";
    public static final String ERROR_SECTOR_NAME_DUPLICATE = "error.sector.name.duplicate";
    public static final String ERROR_SECTOR_HAS_MACHINES = "error.sector.has.machines";
    public static final String ERROR_SECTOR_IN_USE = "error.sector.in.use";
    public static final String ERROR_ACCESS_LEVEL_NOTFOUND = "error.access.level.notfound";
    public static final String ERROR_ROUTE_ALREADY_EXISTS = "error.route.already.exists";
    public static final String ERROR_ROUTE_PRODUCT_REQUIRED = "error.route.product.required";
    public static final String ERROR_ROUTE_STEP_REQUIRED = "error.route.step.required";
    public static final String ERROR_ROUTE_STEP_NOTFOUND = "error.route.step.notfound";
    public static final String ERROR_GLASS_SHEET_NOTFOUND = "error.glass.sheet.notfound";
    public static final String ERROR_GLASS_OPERATOR_NOTFOUND = "error.glass.operator.notfound";
    public static final String ERROR_GLASS_TYPE_DUPLICATE = "error.glass.type.duplicate";
    public static final String ERROR_VALIDATION_FAILED = "error.validation.failed";
    public static final String ERROR_SYSTEM_INTERNAL = "error.system.internal";
    public static final String ERROR_ORDEM_SETOR_NOTFOUND = "error.ordemsetor.notfound";
    public static final String ERROR_APONTAMENTO_QUANTIDADE_ZERO = "error.apontamento.quantidade.zero";
    public static final String ERROR_APONTAMENTO_QUANTIDADE_EXCEDE = "error.apontamento.quantidade.excede";
    public static final String ERROR_APONTAMENTO_REFUGO_MOTIVO_OBRIGATORIO = "error.apontamento.refugo.motivo.obrigatorio";
    public static final String ERROR_ROTEIRO_ETAPA_NAO_CRIADA = "error.roteiro.etapa.naocriada";
    public static final String SUCCESS_APONTAMENTO_LANCADO = "success.apontamento.lancado";
    public static final String SUCCESS_ORDER_CREATED = "success.order.created";
    public static final String ERROR_PRODUCT_WITHOUT_ROUTE = "error.product.without.route";
    public static final String ERROR_ORDER_SECTOR_NOTFOUND = "error.order.sector.notfound";
    public static final String ERROR_ORDER_NOT_AVAILABLE = "error.order.not.available";
    public static final String SUCCESS_ORDER_LIBERADA = "success.order.liberada";
}
