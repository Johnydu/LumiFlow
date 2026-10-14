package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.MaquinaService;
import br.com.lumiflow.service.SetorService;
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
@RequestMapping("/dashboard/maquinas")
public class MaquinaController {

    private final SetorService setorService;
    private final MaquinaService maquinaService;
    private final MessageSource messageSource;

    @GetMapping
    public String maquinas(Model model, Locale locale) {
        try {
            carregarPagina(model);
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de máquinas: {}", e.getMessage(), e);
            model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            model.addAttribute("maquinas", List.of());
            model.addAttribute("setores", List.of());
            if (!model.containsAttribute("maquinaDTO")) {
                model.addAttribute("maquinaDTO", new MaquinaDTO(null, null, null, null));
            }
        }
        return "maquina/CadastroMaquinas";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String novaMaquina(@Valid @ModelAttribute("maquinaDTO") MaquinaDTO dto, BindingResult result,
                              Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            maquinaService.novaMaquina(dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_MACHINE_CREATED, new Object[]{dto.nome()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao criar máquina '{}': {}", dto.nome(), e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("maquinaDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar máquina '{}': {}", dto.nome(), e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("maquinaDTO", dto);
        }

        return "redirect:/dashboard/maquinas";
    }

    @PostMapping("/{id}/excluir")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String deletarMaquina(@PathVariable long id, RedirectAttributes attributes, Locale locale) {
        try {
            maquinaService.deletarMaquina(id);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_MACHINE_DELETED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao excluir máquina id {}: {}", id, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir máquina id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        }

        return "redirect:/dashboard/maquinas";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("maquinaDTO") MaquinaDTO dto,
                         BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            maquinaService.editarMaquina(id, dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_MACHINE_UPDATED, new Object[]{dto.nome()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao editar máquina id {}: {}", id, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("maquinaDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar máquina id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("maquinaDTO", dto);
        }

        return "redirect:/dashboard/maquinas";
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
        return "maquina/CadastroMaquinas";
    }

    private void carregarPagina(Model model) {
        try {
            model.addAttribute("maquinas", maquinaService.listarMaquinas());
        } catch (Exception e) {
            log.error("Erro ao carregar lista de máquinas: {}", e.getMessage(), e);
            model.addAttribute("maquinas", List.of());
        }

        try {
            model.addAttribute("setores", setorService.listarSetores());
        } catch (Exception e) {
            log.error("Erro ao carregar lista de setores para máquinas: {}", e.getMessage(), e);
            model.addAttribute("setores", List.of());
        }

        if (!model.containsAttribute("maquinaDTO")) {
            model.addAttribute("maquinaDTO",
                    new MaquinaDTO(null, null, null, null));
        }
    }
}