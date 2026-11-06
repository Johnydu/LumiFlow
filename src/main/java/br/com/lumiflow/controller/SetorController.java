package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.SetorService;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/dashboard/setores")
public class SetorController {

    private final SetorService setorService;
    private final MessageSource messageSource;

    @GetMapping
    public String setores(
            @RequestParam(value = "nome", required = false) String nome,
            Model model,
            HttpServletRequest request,
            Locale locale) {

        try {
            carregarPagina(model, nome);
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de setores: {}", e.getMessage(), e);
            model.addAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
            model.addAttribute("setores", List.of());
            if (!model.containsAttribute("setorDTO")) {
                model.addAttribute("setorDTO", new SetorDTO(null, null, null));
            }
        }

        // Retorna apenas o fragmento da tabela em requisições AJAX assíncronas
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "setor/Setores :: listaSetores";
        }

        return "setor/Setores";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String novoSetor(
            @Valid @ModelAttribute("setorDTO") SetorDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes attributes,
            Locale locale) {

        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            setorService.novoSetor(dto);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_SECTOR_CREATED, new Object[]{dto.nome()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao criar setor '{}': {}", dto.nome(), e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("setorDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar setor '{}': {}", dto.nome(), e.getMessage(), e);
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
            attributes.addFlashAttribute("setorDTO", dto);
        }

        return "redirect:/dashboard/setores";
    }

    @PostMapping("/{id}/excluir")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String excluirSetor(
            @PathVariable Long id,
            RedirectAttributes attributes,
            Locale locale) {

        try {
            setorService.excluirSetor(id);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_SECTOR_DELETED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao excluir setor id {}: {}", id, e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir setor id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
        }

        return "redirect:/dashboard/setores";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String editarSetor(
            @PathVariable Long id,
            @Valid @ModelAttribute("setorDTO") SetorDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes attributes,
            Locale locale) {

        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            setorService.editarSetor(id, dto);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_SECTOR_UPDATED, new Object[]{dto.nome()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao editar setor id {}: {}", id, e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("setorDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar setor id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
            attributes.addFlashAttribute("setorDTO", dto);
        }

        return "redirect:/dashboard/setores";
    }

    private String erroValidacao(Model model, BindingResult result, Locale locale) {
        carregarPagina(model, null);
        String mensagem = MensagensWeb.deBinding(messageSource, result, locale);
        model.addAttribute("error", mensagem);
        return "setor/Setores";
    }

    private void carregarPagina(Model model, String nome) {
        // Falhas de carga propagam: o chamador mostra o erro ao usuário (nunca uma lista vazia silenciosa).
        if (nome != null && !nome.isBlank()) {
            model.addAttribute("setores", setorService.buscarPorNome(nome.trim()));
        } else {
            model.addAttribute("setores", setorService.listarSetores());
        }

        if (!model.containsAttribute("setorDTO")) {
            model.addAttribute("setorDTO",
                    new SetorDTO(null, null, null));
        }
    }
}