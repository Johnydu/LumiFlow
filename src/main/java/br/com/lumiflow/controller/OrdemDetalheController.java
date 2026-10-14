package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.DividirEtapaDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OrdemDetalheService;
import br.com.lumiflow.service.OrdemProducaoService;
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

import java.util.Locale;

/**
 * Tela de detalhes da ordem. Apenas recebe a requisição e delega ao service;
 * erros de negócio são tratados pelo GlobalExceptionHandler.
 */
@Controller
@RequestMapping("/dashboard/ordens")
@RequiredArgsConstructor
@Slf4j
public class OrdemDetalheController {

    private final OrdemDetalheService ordemDetalheService;
    private final MessageSource messageSource;
    private final OrdemProducaoService ordemProducaoService;

    @GetMapping("/{ordemId}/detalhes")
    @PreAuthorize("hasAnyRole('GESTAO','PCP', 'ADMIN','SUPERVISOR')")
    public String detalhes(@PathVariable Long ordemId, Model model) {
        model.addAttribute("ordem", ordemDetalheService.buscarDetalhe(ordemId));
        return "ordens/DetalhesOrdem";
    }

    @PostMapping("/{ordemId}/etapas/{ordemSetorId}/liberar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTAO', 'SUPERVISOR', 'PCP')")
    public String liberar(@PathVariable Long ordemId,
                          @PathVariable Long ordemSetorId,
                          RedirectAttributes redirectAttributes,
                          Locale locale) {
        log.info("Requisição recebida para liberar etapa ID: {} da ordem ID: {}", ordemSetorId, ordemId);
        try {
            ordemProducaoService.liberarOrdem(ordemSetorId);
            String mensagemSucesso = messageSource.getMessage(AppMessages.SUCCESS_ORDER_LIBERADA, null, locale);
            redirectAttributes.addFlashAttribute("sucesso", mensagemSucesso);
            log.info("Etapa liberada com sucesso para ID: {}", ordemSetorId);
        } catch (BusinessException e) {
            log.warn("Bloqueio de regra de negócio ao liberar etapa {}: {}", ordemSetorId, e.getMessage());
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao processar liberação da etapa {}: {}", ordemSetorId, e.getMessage(), e);
            String mensagemErro = messageSource.getMessage(AppMessages.ERROR_SYSTEM_INTERNAL, null, locale);
            redirectAttributes.addFlashAttribute("erro", mensagemErro);
        }
        return "redirect:/dashboard/ordens/" + ordemId + "/detalhes";
    }

    @PostMapping("/{ordemId}/etapas/{ordemSetorId}/dividir")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTAO', 'SUPERVISOR', 'PCP')")
    public String dividir(@PathVariable Long ordemId,
                          @PathVariable Long ordemSetorId,
    @Valid @ModelAttribute("dividirDTO") DividirEtapaDTO dto,
    BindingResult result,
    Model model,
    RedirectAttributes redirectAttributes,
    Locale locale) {
        if (result.hasErrors()) {
            model.addAttribute("ordem", ordemDetalheService.buscarDetalhe(ordemId));
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
            return "ordens/DetalhesOrdem";
        }

        log.info("Requisição recebida para dividir etapa ID: {} da ordem ID: {}", ordemSetorId, ordemId);
        try {
            ordemProducaoService.dividirEtapa(ordemSetorId, dto);
            String mensagemSucesso = messageSource.getMessage(AppMessages.SUCCESS_ORDER_DIVIDED, null, locale);
            redirectAttributes.addFlashAttribute("sucesso", mensagemSucesso);
            log.info("Etapa dividida com sucesso para ID: {}", ordemSetorId);
        } catch (BusinessException e) {
            log.warn("Bloqueio de regra de negócio ao dividir etapa {}: {}", ordemSetorId, e.getMessage());
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao processar divisão da etapa {}: {}", ordemSetorId, e.getMessage(), e);
            String mensagemErro = messageSource.getMessage(AppMessages.ERROR_SYSTEM_INTERNAL, null, locale);
            redirectAttributes.addFlashAttribute("erro", mensagemErro);
        }
        return "redirect:/dashboard/ordens/" + ordemId + "/detalhes";
    }
}