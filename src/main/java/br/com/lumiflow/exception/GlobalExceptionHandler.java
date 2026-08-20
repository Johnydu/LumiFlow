package br.com.lumiflow.exception;

import br.com.lumiflow.config.AppMessages;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Centralizador global de tratamento de exceções com logging estruturado.
 * 
 * Responsabilidades:
 * - Interceptar exceções não capturadas em Controllers
 * - Logar exceções com contexto apropriado (nível, mensagem, stack trace)
 * - Retornar páginas de erro amigáveis ao usuário
 * - Proteger informações sensíveis (nunca expor stack traces em produção)
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata ResourceNotFoundException (Recurso não encontrado - 404)
     * 
     * Nível: WARN - comportamento esperado, não é crítico
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request,
            Model model) {

        String requestUri = request.getRequestURI();
        log.warn("Recurso não encontrado. URI: {}, Mensagem: {}", 
                 requestUri, ex.getMessage());

        model.addAttribute("erro", AppMessages.ERROR_USER_NOT_FOUND);
        return "error/404";
    }

    /**
     * Trata BusinessException (Violação de regra de negócio - 400)
     * 
     * Nível: WARN - falha esperada de lógica de negócio
     * Exemplos: usuário duplicado, senha inválida, validação falhou
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(
            BusinessException ex,
            HttpServletRequest request,
            Model model) {

        String requestUri = request.getRequestURI();
        log.warn("Erro de negócio. URI: {}, Mensagem: {}", 
                 requestUri, ex.getMessage());

        model.addAttribute("erro", ex.getMessage());
        return "error/business";
    }

    /**
     * Trata UsuarioNaoEncontradoException (Usuário não existe)
     * 
     * Nível: WARN - falha esperada ao buscar usuário
     */
    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public String handleUsuarioNaoEncontrado(
            UsuarioNaoEncontradoException ex,
            HttpServletRequest request,
            Model model) {

        String requestUri = request.getRequestURI();
        log.warn("Usuário não encontrado. URI: {}, Mensagem: {}", 
                 requestUri, ex.getMessage());

        model.addAttribute("erro", ex.getMessage());
        return "error/404";
    }

    /**
     * Trata OrdemProducaoException (Erro específico de ordem de produção)
     * 
     * Nível: WARN/ERROR - depende da severidade
     */
    @ExceptionHandler(OrdemProducaoException.class)
    public String handleOrdemProducao(
            OrdemProducaoException ex,
            HttpServletRequest request,
            Model model) {

        String requestUri = request.getRequestURI();
        log.warn("Erro na ordem de produção. URI: {}, Mensagem: {}", 
                 requestUri, ex.getMessage());

        model.addAttribute("erro", ex.getMessage());
        return "error/business";
    }

    /**
     * Trata qualquer exceção não mapeada (500)
     * 
     * Nível: ERROR - erro crítico não esperado
     * Inclui: stack trace para debugging e contexto da requisição
     * Nunca expõe detalhes internos ao usuário
     */
    @ExceptionHandler(Exception.class)
    public String handleGeneric(
            Exception ex,
            HttpServletRequest request,
            Model model) {

        String requestUri = request.getRequestURI();
        String exceptionType = ex.getClass().getSimpleName();
        
        log.error("Erro inesperado não tratado. URI: {}, Tipo: {}, Mensagem: {}", 
                  requestUri, exceptionType, ex.getMessage(), ex);

        model.addAttribute("erro", AppMessages.ERROR_INTERNAL_SERVER);
        
        return "error/500";
    }
}
