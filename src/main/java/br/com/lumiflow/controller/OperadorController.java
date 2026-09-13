package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.SetorService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@AllArgsConstructor
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
            jakarta.servlet.http.HttpServletRequest request) {

        carregarPagina(model, nome, setorId);

        // Se for uma requisição AJAX, retorna apenas o fragmento da lista
        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "operador/Operadores :: listaOperadores";
        }

        return "operador/Operadores";
    }

    private void carregarPagina(Model model, String nome, Long setorId) {
        if ((nome != null && !nome.isBlank()) || setorId != null) {
            model.addAttribute("operadores", operadorService.buscarOperadores(nome, setorId));
        } else {
            model.addAttribute("operadores", operadorService.listarOperadores());
        }

        if (!model.containsAttribute("operadorDTO")) {
            model.addAttribute("operadorDTO", new OperadorDTO(null, null, null, null));
        }
        if (!model.containsAttribute("listaSetores")) {
            model.addAttribute("listaSetores", setorService.listarSetores());
        }
    }

    @PostMapping
    public String novoOperador(@Valid @ModelAttribute("operadorDTO") OperadorDTO dto, BindingResult result,
                               Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) return erroValidacao(model, locale);
        operadorService.novoOperador(dto);
        attributes.addFlashAttribute("success",
                messageSource.getMessage(AppMessages.SUCCESS_OPERATOR_CREATED, new Object[]{dto.nome()}, locale));
        return "redirect:/dashboard/operadores";
    }

    @PostMapping("/{id}/excluir")
    public String excluirOperador(@PathVariable Long id, RedirectAttributes attributes, Locale locale) {

        operadorService.excluirOperador(id);
        attributes.addFlashAttribute("success",
                messageSource.getMessage(AppMessages.SUCCESS_OPERATOR_DELETED, null, locale));
        return "redirect:/dashboard/operadores";
    }

    @PostMapping("/{id}/editar")
    public String editarOperador(@PathVariable long id, @Valid @ModelAttribute("operadorDTO") OperadorDTO dto,
                                 BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {

        if (result.hasErrors()) return erroValidacao(model, locale);
        operadorService.editarOperador(id, dto);
        attributes.addFlashAttribute("success",
                messageSource.getMessage(AppMessages.SUCCESS_OPERATOR_UPDATED, new Object[]{dto.nome()}, locale));
        return "redirect:/dashboard/operadores";
    }

    private String erroValidacao(Model model, Locale locale) {
        carregarPagina(model, null, null);
        model.addAttribute("error",
                messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        return "operador/Operadores";
    }

    private void carregarPagina(Model model) {

        model.addAttribute("operadores", operadorService.listarOperadores());
        model.addAttribute("listaSetores", setorService.listarSetores());
        if (!model.containsAttribute("operadorDTO"))
            model.addAttribute("operadorDTO",
                    new OperadorDTO(null, null, null, null));
    }
}
