package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.DividirEtapaDTO;
import br.com.lumiflow.dto.ordens.OrdemFiltroDTO;
import br.com.lumiflow.dto.ordens.OrdemListagemDTO;
import br.com.lumiflow.dto.ordens.ResumoSetorDTO;
import br.com.lumiflow.entity.Setor;
import org.springframework.security.core.Authentication;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OrdemProducaoService;
import br.com.lumiflow.service.OrdemSetorService;
import br.com.lumiflow.service.SetorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
@RequestMapping("/dashboard/ordens-setor")
@RequiredArgsConstructor
public class OrdemSetorController {

    private final OrdemSetorService ordemSetorService;
    private final OrdemProducaoService ordemProducaoService;
    private final SetorService setorService;
    private final MessageSource messageSource;

    @GetMapping
    public String listarOrdensSetor(
            @RequestParam(value = "busca", required = false)
            String busca,
            Model model,
            Locale locale) {

        try {
            List<ResumoSetorDTO> resumoSetores = ordemSetorService.obterResumoSetores(busca);
            model.addAttribute("resumoSetores", resumoSetores);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar resumo dos setores: {}", e.getMessage(), e);
            model.addAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
            model.addAttribute("resumoSetores", List.of());
        }

        return "ordens/ListaOrdemSetores";
    }

    @GetMapping("/{setorId}")
    public String listarOrdensPorSetorId(@PathVariable("setorId") Long setorId,
                                         @ModelAttribute OrdemFiltroDTO filtro,
                                         @RequestParam(value = "page", defaultValue = "0") int page,
                                         Model model,
                                         RedirectAttributes attributes,
                                         Locale locale) {

        try {
            Page<OrdemListagemDTO> pagina = ordemProducaoService.listarPorSetorIdEFiltros(setorId, filtro, page);
            Setor setor = setorService.buscarSetorPorId(setorId);

            model.addAttribute("pagina", pagina);
            model.addAttribute("ordens", pagina.getContent());
            model.addAttribute("setorAtual", setor != null ? setor.getNome() : "Desconhecido");
            model.addAttribute("setorIdAtual", setorId);
            model.addAttribute("buscaAtual", filtro.busca());
            model.addAttribute("statusAtual", filtro.status());
            model.addAttribute("totalOrdens", pagina.getTotalElements());
        } catch (BusinessException e) {
            // Setor inexistente: volta para a lista de setores com o aviso, em vez de uma tela vazia.
            log.warn("Erro de negócio ao listar ordens do setor id {}: {}", setorId, e.getMessage());
            attributes.addFlashAttribute("error", MensagensWeb.de(messageSource, e, locale));
            return "redirect:/dashboard/ordens-setor";
        } catch (Exception e) {
            log.error("Erro inesperado ao listar ordens para o setor id {}: {}", setorId, e.getMessage(), e);
            model.addAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
            model.addAttribute("ordens", List.of());
            model.addAttribute("pagina", Page.empty());
            model.addAttribute("setorAtual", "Desconhecido");
            model.addAttribute("setorIdAtual", setorId);
            model.addAttribute("totalOrdens", 0);
        }

        return "ordens/ListaOrdem";
    }

    @PostMapping("/{setorId}/ordens/{ordemSetorId}/liberar")
    @PreAuthorize("hasAnyRole('ADMIN','PCP','GESTAO','SUPERVISOR')")
    public String liberarOrdem(@PathVariable Long setorId, @PathVariable Long ordemSetorId,
                               @RequestParam(value = "ordemId", required = false) Long ordemId,
                               @RequestParam(value = "retorno", required = false) String retorno,
                               Authentication authentication,
                               RedirectAttributes attributes, Locale locale) {

        try {
            ordemProducaoService.liberarOrdem(ordemSetorId);
            log.info("Etapa {} liberada por {}", ordemSetorId, authentication.getName());
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_ORDER_LIBERADA, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao liberar ordem setor id {}: {}", ordemSetorId, e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (ObjectOptimisticLockingFailureException e) {
            // tratado pelo GlobalExceptionHandler com a mensagem de edição concorrente
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao liberar ordem setor id {}: {}", ordemSetorId, e.getMessage(), e);
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
        }

        return destino(setorId, ordemId, retorno);
    }

    @PostMapping("/{setorId}/ordens/{ordemSetorId}/pausar")
    @PreAuthorize("hasAnyRole('ADMIN','PCP','GESTAO','SUPERVISOR')")
    public String pausarEtapa(@PathVariable Long setorId, @PathVariable Long ordemSetorId,
                              @RequestParam(value = "ordemId", required = false) Long ordemId,
                              @RequestParam(value = "retorno", required = false) String retorno,
                              Authentication authentication,
                              RedirectAttributes attributes, Locale locale) {

        try {
            ordemProducaoService.pausarEtapa(ordemSetorId);
            log.info("Etapa {} devolvida para DISPONIVEL por {}", ordemSetorId, authentication.getName());
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_ORDER_PAUSADA, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao pausar ordem setor id {}: {}", ordemSetorId, e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (ObjectOptimisticLockingFailureException e) {
            // tratado pelo GlobalExceptionHandler com a mensagem de edição concorrente
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao pausar ordem setor id {}: {}", ordemSetorId, e.getMessage(), e);
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
        }

        return destino(setorId, ordemId, retorno);
    }

    @PostMapping("/{setorId}/ordens/{ordemSetorId}/dividir")
    @PreAuthorize("hasAnyRole('ADMIN','PCP','GESTAO','SUPERVISOR')")
    public String dividirEtapa(@PathVariable Long setorId, @PathVariable Long ordemSetorId,
                               @Valid @ModelAttribute("dividirDTO") DividirEtapaDTO dto, BindingResult result,
                               @RequestParam(value = "ordemId", required = false) Long ordemId,
                               @RequestParam(value = "retorno", required = false) String retorno,
                               Authentication authentication,
                               RedirectAttributes attributes, Locale locale) {

        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
            return destino(setorId, ordemId, retorno);
        }

        try {
            ordemProducaoService.dividirEtapa(ordemSetorId, dto);
            log.info("Etapa {} dividida por {}", ordemSetorId, authentication.getName());
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_ORDER_DIVIDED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao dividir etapa da ordem setor id {}: {}", ordemSetorId, e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (ObjectOptimisticLockingFailureException e) {
            // tratado pelo GlobalExceptionHandler com a mensagem de edição concorrente
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao dividir etapa da ordem setor id {}: {}", ordemSetorId, e.getMessage(), e);
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
        }

        return destino(setorId, ordemId, retorno);
    }

    /**
     * Destino após a ação: volta para os detalhes da ordem quando a ação veio de lá;
     * caso contrário, volta para a lista do setor. O destino é montado só com ids numéricos,
     * sem aceitar URL vinda do formulário (evita open redirect).
     */
    private String destino(Long setorId, Long ordemId, String retorno) {
        if ("detalhes".equals(retorno) && ordemId != null) {
            return "redirect:/dashboard/ordens/" + ordemId + "/detalhes";
        }
        return "redirect:/dashboard/ordens-setor/" + setorId;
    }





}
