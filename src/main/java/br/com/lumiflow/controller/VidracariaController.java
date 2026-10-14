package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.dto.vidracaria.MovimentacaoVidroDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.VidracariaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/dashboard/vidracaria")
public class VidracariaController {

    private final VidracariaService vidracariaService;
    private final OperadorService operadorService;
    private final MessageSource messageSource;

    @GetMapping
    public String vidracaria(Model model, Locale locale) {
        try {
            carregarPagina(model);
            return "vidracaria/Vidracaria";
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de vidraçaria: {}", e.getMessage(), e);
            model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            return "vidracaria/Vidracaria";
        }
    }

    private String erroValidacao(Model model, BindingResult result, Locale locale) {
        carregarPagina(model);
        String mensagem = result.getAllErrors().stream()
                .findFirst()
                .map(error -> {
                    String defaultMessage = error.getDefaultMessage();
                    return defaultMessage == null ? "" :
                            messageSource.getMessage(defaultMessage, null, defaultMessage, locale);
                })
                .orElseGet(() -> messageSource.getMessage(
                        AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        model.addAttribute("error", mensagem);
        return "vidracaria/Vidracaria";
    }

    @PostMapping("/movimentar")
    public String registrarMovimentacao(@Valid @ModelAttribute("movimentacaoDTO") MovimentacaoVidroDTO dto,
                                        BindingResult result, Model model,
                                        RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            vidracariaService.registrarMovimentacao(dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_GLASS_MOVEMENT_CREATED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao registrar movimentação de vidro: {}", e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("movimentacaoDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao registrar movimentação de vidro: {}", e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("movimentacaoDTO", dto);
        }

        return "redirect:/dashboard/vidracaria";
    }


    @PostMapping("/cadastrar-chapa")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String cadastrarChapa(@Valid @ModelAttribute("chapaDTO") ChapaVidroDTO dto, BindingResult result,
                                 Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            vidracariaService.cadastrarChapa(dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_GLASS_TYPE_CREATED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao cadastrar chapa de vidro: {}", e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("chapaDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao cadastrar chapa de vidro: {}", e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("chapaDTO", dto);
        }

        return "redirect:/dashboard/vidracaria";
    }

    private void carregarPagina(Model model) {
        try {
            model.addAttribute("saldos", vidracariaService.obterSaldosAtuais());
            model.addAttribute("chapas", vidracariaService.listarChapas());
            model.addAttribute("historico", vidracariaService.listarHistorico());
            model.addAttribute("operadores", operadorService.listarOperadores());
        } catch (Exception e) {
            log.error("Erro ao carregar dados auxiliares da página de vidraçaria: {}", e.getMessage(), e);
            model.addAttribute("saldos", List.of());
            model.addAttribute("chapas", List.of());
            model.addAttribute("historico", List.of());
            model.addAttribute("operadores", List.of());
        }
    }

    @GetMapping("/filtrar-historico")
    public String filtrarHistoricoAjax(
            @RequestParam(required = false) String tipoMovimentacao,
            @RequestParam(required = false) String tipoVidroFiltro,
            Model model, Locale locale) {

        try {
            String tipoMov = (tipoMovimentacao != null && tipoMovimentacao.isBlank()) ? null : tipoMovimentacao;
            String tipoVidro = (tipoVidroFiltro != null && tipoVidroFiltro.isBlank()) ? null : tipoVidroFiltro;

            model.addAttribute("historico", vidracariaService.listarHistoricoFiltrado(tipoMov, tipoVidro));
        } catch (Exception e) {
            log.error("Erro ao filtrar histórico de movimentação de vidro via AJAX: {}", e.getMessage(), e);
            model.addAttribute("historico", List.of());
        }

        // Aponta diretamente para o arquivo isolado do fragmento
        return "vidracaria/tabela-historico-movimentacao-vidro :: tabela";
    }
}