package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OperadorService;
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
@RequestMapping("/dashboard/operadores")
public class OperadorController {

    private final OperadorService operadorService;
    private final SetorService setorService;
    private final MessageSource messageSource;

    @GetMapping
    public String listarOperadores(
            @RequestParam(value = "nome", required = false) String nome,
            @RequestParam(value = "setorId", required = false) Long setorId,
            Model model,
            HttpServletRequest request,
            Locale locale) {

        try {
            carregarPagina(model, nome, setorId);
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de operadores: {}", e.getMessage(), e);
            model.addAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            model.addAttribute("operadores", List.of());
            model.addAttribute("listaSetores", List.of());
            if (!model.containsAttribute("operadorDTO")) {
                model.addAttribute("operadorDTO",
                        new OperadorDTO(null, null, null, null));
            }
        }

        // Se for uma requisição AJAX, retorna apenas o fragmento da lista
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "operador/Operadores :: listaOperadores";
        }

        return "operador/Operadores";
    }

    private void carregarPagina(Model model, String nome, Long setorId) {
        try {
            if ((nome != null && !nome.isBlank()) || setorId != null) {
                model.addAttribute("operadores", operadorService.buscarOperadores(nome, setorId));
            } else {
                model.addAttribute("operadores", operadorService.listarOperadores());
            }
        } catch (Exception e) {
            log.error("Erro ao carregar operadores: {}", e.getMessage(), e);
            model.addAttribute("operadores", List.of());
        }

        try {
            if (!model.containsAttribute("listaSetores")) {
                model.addAttribute("listaSetores", setorService.listarSetores());
            }
        } catch (Exception e) {
            log.error("Erro ao carregar setores para operadores: {}", e.getMessage(), e);
            model.addAttribute("listaSetores", List.of());
        }

        if (!model.containsAttribute("operadorDTO")) {
            model.addAttribute("operadorDTO",
                    new OperadorDTO(null, null, null, null));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String novoOperador(@Valid @ModelAttribute("operadorDTO") OperadorDTO dto, BindingResult result,
                               Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, locale);
        }

        try {
            operadorService.novoOperador(dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_OPERATOR_CREATED, new Object[]{dto.nome()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao criar operador '{}': {}", dto.nome(), e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("operadorDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar operador '{}': {}", dto.nome(), e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("operadorDTO", dto);
        }

        return "redirect:/dashboard/operadores";
    }

    @PostMapping("/{id}/excluir")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String excluirOperador(@PathVariable Long id, RedirectAttributes attributes, Locale locale) {
        try {
            operadorService.excluirOperador(id);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_OPERATOR_DELETED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao excluir operador id {}: {}", id, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir operador id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        }

        return "redirect:/dashboard/operadores";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String editarOperador(@PathVariable long id, @Valid @ModelAttribute("operadorDTO") OperadorDTO dto,
                                 BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {

        if (result.hasErrors()) {
            return erroValidacao(model, locale);
        }

        try {
            operadorService.editarOperador(id, dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_OPERATOR_UPDATED, new Object[]{dto.nome()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao editar operador id {}: {}", id, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("operadorDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar operador id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("operadorDTO", dto);
        }

        return "redirect:/dashboard/operadores";
    }

    private String erroValidacao(Model model, Locale locale) {
        carregarPagina(model, null, null);
        model.addAttribute("error",
                messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        return "operador/Operadores";
    }
}