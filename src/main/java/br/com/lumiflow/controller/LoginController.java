package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", AppMessages.ERROR_USER_INVALID_CREDENTIALS);
        }
        if (logout != null) {
            model.addAttribute("success", AppMessages.SUCCESS_LOGOUT);
        }
        return "usuario/Login";
    }
}
