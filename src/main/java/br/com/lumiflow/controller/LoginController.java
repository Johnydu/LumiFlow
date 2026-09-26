package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final MessageSource messageSource;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model,
                        Locale locale) {

        try {
            if (error != null) {
                String msgErro = messageSource.getMessage(AppMessages.ERROR_USER_INVALID_CREDENTIALS, null, locale);
                model.addAttribute("error", msgErro);
                log.debug("Parâmetro 'error' detectado na tela de login.");
            }

            if (logout != null) {
                String msgLogout = messageSource.getMessage(AppMessages.SUCCESS_USER_LOGOUT, null, locale);
                model.addAttribute("success", msgLogout);
                log.debug("Parâmetro 'logout' detectado na tela de login.");
            }
        } catch (Exception e) {
            log.error("Erro inesperado ao processar mensagens da página de login: {}", e.getMessage(), e);
            model.addAttribute("error", "Ocorreu um erro ao carregar as mensagens do sistema.");
        }

        return "usuario/Login";
    }
}