package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/dashboard/configuracoes")
public class ConfiguracaoController {

    private final MessageSource messageSource;

    @GetMapping
    public String paginaConfiguracoes(Model model, HttpServletRequest request, Locale locale) {
        try {
            // Caso precise carregar alguma preferência salva do utilizador no futuro
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de configurações: {}", e.getMessage(), e);
            model.addAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        }

        // Suporte a requisições AJAX seguindo o padrão dos outros modais/telas
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "configuracao/Configuracoes :: conteudoConfiguracoes";
        }

        return "configuracao/Configuracoes";
    }
}