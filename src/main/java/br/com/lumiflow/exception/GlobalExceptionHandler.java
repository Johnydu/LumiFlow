package br.com.lumiflow.exception;

import br.com.lumiflow.config.AppMessages;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(
            BusinessException ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            Locale locale) {

        String mensagem = messageSource.getMessage(
                ex.getMessageKey(),
                ex.getParameters(),
                locale
        );
        redirectAttributes.addFlashAttribute("error", mensagem);

        // Pega a URL da página onde o usuário estava antes de disparar a exceção
        String referer = request.getHeader("Referer");

        // Se existir a página anterior, redireciona para ela; caso contrário, vai para o dashboard
        return referer != null ? "redirect:" + referer : "redirect:/dashboard";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(
            Exception ex,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            Locale locale) {

        String mensagem = messageSource.getMessage(
                AppMessages.ERROR_SYSTEM_INTERNAL,
                null,
                locale
        );
        redirectAttributes.addFlashAttribute("error", mensagem);

        String referer = request.getHeader("Referer");
        return referer != null ? "redirect:" + referer : "redirect:/dashboard";
    }
}