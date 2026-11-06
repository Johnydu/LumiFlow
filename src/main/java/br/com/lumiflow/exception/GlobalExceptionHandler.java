package br.com.lumiflow.exception;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.controller.MensagensWeb;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.util.Locale;

/**
 * Tratamento global de exceções.
 * <ul>
 *   <li>Páginas HTML: flash "error" + redirect seguro (Referer interno ou destino padrão).</li>
 *   <li>Endpoints REST/AJAX ({@code @RestController}, {@code @ResponseBody} ou {@code ResponseEntity},
 *       além de {@code /api/**}): {@link ProblemDetail} com o status HTTP correto, sem redirect.</li>
 *   <li>Exceções padrão do Spring MVC (405, 400 de tipo de parâmetro, 404 de recurso, etc.):
 *       herdadas de {@link ResponseEntityExceptionHandler}, mantendo o status original.</li>
 * </ul>
 */
@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String DESTINO_PADRAO = "redirect:/dashboard/ordens-setor";

    private final MessageSource messageSource;

    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) {
        throw ex;
    }

    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(
            BusinessException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            Locale locale) {

        String mensagem = MensagensWeb.de(messageSource, ex, locale);
        if (isRequisicaoRest(request)) {
            return problema(statusDe(ex.getMessageKey()), mensagem);
        }
        redirectAttributes.addFlashAttribute("error", mensagem);
        return redirecionarComSeguranca(request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public Object handleConcorrencia(RedirectAttributes redirectAttributes,
                                     HttpServletRequest request, Locale locale) {
        String mensagem = messageSource.getMessage(AppMessages.ERROR_CONCURRENT_UPDATE, null, locale);
        if (isRequisicaoRest(request)) {
            return problema(HttpStatus.CONFLICT, mensagem);
        }
        redirectAttributes.addFlashAttribute("error", mensagem);
        return redirecionarComSeguranca(request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleInesperado(
            Exception ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            Locale locale) {

        log.error("Erro não tratado na requisição [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        String mensagem = messageSource.getMessage(AppMessages.ERROR_SYSTEM_INTERNAL, null, locale);
        if (isRequisicaoRest(request)) {
            return problema(HttpStatus.INTERNAL_SERVER_ERROR, mensagem);
        }
        redirectAttributes.addFlashAttribute("error", mensagem);
        return redirecionarComSeguranca(request);
    }

    /** Status HTTP derivado da chave da mensagem de negócio (convenção das chaves do messages.properties). */
    static HttpStatus statusDe(String chave) {
        if (chave == null) {
            return HttpStatus.BAD_REQUEST;
        }
        if (chave.contains("notfound")) {
            return HttpStatus.NOT_FOUND;
        }
        if (chave.contains("duplicate") || chave.contains("already.") || chave.contains(".in.use")
                || chave.contains(".has.") || chave.contains("concurrent")
                || chave.contains("not.available") || chave.contains("not.pausable")) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private ResponseEntity<ProblemDetail> problema(HttpStatus status, String detalhe) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ProblemDetail.forStatusAndDetail(status, detalhe));
    }

    /** REST/AJAX: handler @RestController, método @ResponseBody, retorno ResponseEntity ou URL /api/**. */
    private boolean isRequisicaoRest(HttpServletRequest request) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod metodo
                && (metodo.hasMethodAnnotation(ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(metodo.getBeanType(), ResponseBody.class)
                || HttpEntity.class.isAssignableFrom(metodo.getReturnType().getParameterType()))) {
            return true;
        }
        String uri = request.getRequestURI();
        String contexto = request.getContextPath();
        String semContexto = uri.startsWith(contexto) ? uri.substring(contexto.length()) : uri;
        return semContexto.startsWith("/api/");
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

    /**
     * Aceita só caminho relativo ou URL http(s) do mesmo host. Rejeita URIs opacas
     * (ex.: {@code javascript:...}, {@code mailto:...}), esquemas não-http e hosts diferentes.
     */
    private boolean isCaminhoInternoSeguro(String referer, HttpServletRequest request) {
        try {
            URI uri = URI.create(referer);
            if (uri.isOpaque()) {
                return false;
            }
            String scheme = uri.getScheme();
            if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
                return false;
            }
            String host = uri.getHost();
            if (host == null) {
                // Sem host: só vale caminho relativo (sem esquema nem autoridade).
                return scheme == null && uri.getRawAuthority() == null;
            }
            return host.equalsIgnoreCase(request.getServerName());
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
