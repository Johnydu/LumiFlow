package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.service.LancamentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/dashboard/lancamentos")
@RequiredArgsConstructor
public class HistoricoLancamentosController {

    private final LancamentoService lancamentoService;
    private final MessageSource messageSource;

    @GetMapping
    public String lancamentos(Model model, Locale locale) {
        try {
            carregarPagina(model);
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de histórico de lançamentos: {}", e.getMessage(), e);
            model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            model.addAttribute("historicos", List.of());
        }

        return "historico/HistoricoLancamento";
    }

    private void carregarPagina(Model model) {
        try {
            model.addAttribute("historicos", lancamentoService.listarHistoricoLancamentos());
        } catch (Exception e) {
            log.error("Erro ao carregar lista de histórico de lançamentos: {}", e.getMessage(), e);
            model.addAttribute("historicos", List.of());
        }
    }
}