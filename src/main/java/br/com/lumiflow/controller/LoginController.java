package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final MessageSource messageSource;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model,
                        Locale locale) {

        if (error != null) {
            String msgErro = messageSource.getMessage(AppMessages.ERROR_USER_INVALID_CREDENTIALS, null, locale);
            model.addAttribute("error", msgErro);
        }

        if (logout != null) {
            String msgLogout = messageSource.getMessage(AppMessages.SUCCESS_USER_LOGOUT, null, locale);
            model.addAttribute("success", msgLogout);
        }

        return "usuario/Login";
    }
}