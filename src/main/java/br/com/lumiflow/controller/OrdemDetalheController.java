package br.com.lumiflow.controller;

import br.com.lumiflow.dto.ordens.DividirEtapaDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OrdemDetalheService;
import br.com.lumiflow.service.OrdemProducaoService;
import br.com.lumiflow.service.OrdemSetorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tela de detalhes da ordem. Apenas recebe a requisição e delega ao service;
 * erros de negócio (ordem inexistente) são tratados pelo GlobalExceptionHandler.
 */
@Controller
@RequestMapping("/dashboard/ordens")
@RequiredArgsConstructor
@Slf4j
public class OrdemDetalheController {

    private final OrdemDetalheService ordemDetalheService;
    private final MessageSource messageSource;
    private final OrdemProducaoService ordemProducaoService;
    private final OrdemSetorService ordemSetorService;

    @GetMapping("/{ordemId}/detalhes")
    @PreAuthorize("hasAnyRole('GESTAO','PCP', 'ADMIN','SUPERVISOR')")
    public String detalhes(@PathVariable Long ordemId, Model model) {
        model.addAttribute("ordem", ordemDetalheService.buscarDetalhe(ordemId));
        return "ordens/DetalhesOrdem";
    }

    @PostMapping("/{setorId}/ordens/{ordemSetorId}/liberar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTAO', 'SUPERVISOR', 'PCP')")
    public String liberar(@PathVariable Long setorId,
                          @PathVariable Long ordemSetorId,
                          RedirectAttributes redirectAttributes,
                          @RequestHeader(value = "referer", required = false) String referer) {
        log.info("Requisição recebida para liberar etapa (OrdemSetor ID: {}) no Setor ID: {}", ordemSetorId, setorId);
        try {
            ordemProducaoService.liberarOrdem(ordemSetorId);
            redirectAttributes.addFlashAttribute("sucesso", "Etapa liberada com sucesso!");
            log.info("Etapa liberada com sucesso para OrdemSetor ID: {}", ordemSetorId);
        } catch (BusinessException e) {
            log.warn("Bloqueio de regra de negócio ao liberar etapa {}: {}", ordemSetorId, e.getMessage());
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao processar liberação da etapa {}: {}", ordemSetorId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro inesperado ao tentar liberar a etapa.");
        }
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }

    @PostMapping("/{setorId}/ordens/{ordemSetorId}/dividir")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTAO', 'SUPERVISOR', 'PCP')")
    public String dividir(@PathVariable Long setorId,
                          @PathVariable Long ordemSetorId,
                          @ModelAttribute DividirEtapaDTO dto,
                          RedirectAttributes redirectAttributes,
                          @RequestHeader(value = "referer", required = false) String referer) {
        log.info("Requisição recebida para dividir etapa (OrdemSetor ID: {}) no Setor ID: {}", ordemSetorId, setorId);
        try {
            ordemProducaoService.dividirEtapa(ordemSetorId, dto);
            redirectAttributes.addFlashAttribute("sucesso", "Etapa dividida com sucesso!");
            log.info("Etapa dividida com sucesso para OrdemSetor ID: {}", ordemSetorId);
        } catch (BusinessException e) {
            log.warn("Bloqueio de regra de negócio ao dividir etapa {}: {}", ordemSetorId, e.getMessage());
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao processar divisão da etapa {}: {}", ordemSetorId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("erro", "Ocorreu um erro inesperado ao tentar dividir a etapa.");
        }
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }
}
