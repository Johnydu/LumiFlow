package br.com.lumiflow.exception;

import br.com.lumiflow.config.AppMessages;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.util.Locale;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private static final String DESTINO_PADRAO = "redirect:/dashboard/ordens-setor";

    private final MessageSource messageSource;

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) {
        throw ex;
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(
            BusinessException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            Locale locale) {

        String mensagem = messageSource.getMessage(ex.getMessageKey(), ex.getArgs(), ex.getMessageKey(), locale);
        redirectAttributes.addFlashAttribute("error", mensagem);

        return redirecionarComSeguranca(request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleConcorrencia(RedirectAttributes redirectAttributes,
                                     HttpServletRequest request, Locale locale) {
        String mensagem = messageSource.getMessage(AppMessages.ERROR_CONCURRENT_UPDATE, null, locale);
        redirectAttributes.addFlashAttribute("error", mensagem);
        return redirecionarComSeguranca(request);
    }

    @ExceptionHandler(Exception.class)
    public String handleException(
            Exception ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            Locale locale) {

        log.error("Erro não tratado na requisição [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        String mensagem = messageSource.getMessage(AppMessages.ERROR_SYSTEM_INTERNAL, null, locale);
        redirectAttributes.addFlashAttribute("error", mensagem);

        return redirecionarComSeguranca(request);
    }

    /**
     * Volta para a página de origem somente quando ela é segura e diferente da requisição
     * que falhou. Redirecionar para a própria página que gerou o erro causa loop infinito.
     */
    private String redirecionarComSeguranca(HttpServletRequest request) {
        String referer = request.getHeader("Referer");

        if (referer != null
                && isCaminhoInternoSeguro(referer, request)
                && !apontaParaRequisicaoAtual(referer, request)) {
            return "redirect:" + referer;
        }

        return DESTINO_PADRAO;
    }

    private boolean isCaminhoInternoSeguro(String referer, HttpServletRequest request) {
        try {
            URI uri = URI.create(referer);
            String host = uri.getHost();
            return host == null || host.equalsIgnoreCase(request.getServerName());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean apontaParaRequisicaoAtual(String referer, HttpServletRequest request) {
        try {
            return request.getRequestURI().equals(URI.create(referer).getPath());
        } catch (Exception e) {
            return false;
        }
    }
}