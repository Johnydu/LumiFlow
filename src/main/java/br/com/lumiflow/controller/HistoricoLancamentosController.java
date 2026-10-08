package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.security.UsuarioDetails;
import br.com.lumiflow.service.LancamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/dashboard/lancamentos")
@RequiredArgsConstructor
public class HistoricoLancamentosController {

    private static final int TAMANHO_PAGINA = 50;

    private final LancamentoService lancamentoService;
    private final MessageSource messageSource;

    @GetMapping
    public String lancamentos(@RequestParam(value = "page", defaultValue = "0") int page,
                              @AuthenticationPrincipal UsuarioDetails usuario,
                              Model model, Locale locale) {
        try {
            model.addAttribute("pagina",
                    lancamentoService.listarHistoricoLancamentos(usuario, page, TAMANHO_PAGINA));
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de histórico de lançamentos: {}", e.getMessage(), e);
            model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            model.addAttribute("pagina", Page.empty());
        }

        return "historico/HistoricoLancamento";
    }
}