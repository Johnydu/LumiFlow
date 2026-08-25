package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.service.MaquinaService;
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
@RequestMapping("/dashboard/maquinas")
public class MaquinaController {
    private final SetorService setorService;
    private final MaquinaService maquinaService;
    private final MessageSource messageSource;

    @GetMapping
    public String maquinas(Model model) {
        carregarPagina(model);
        return "maquina/CadastroMaquinas";
    }

    @PostMapping
    public String novaMaquina(@Valid @ModelAttribute("maquinaDTO") MaquinaDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) return erroValidacao(model, locale);
        maquinaService.novaMaquina(dto);
        attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_MACHINE_CREATED, new Object[]{dto.nome()}, locale));
        return "redirect:/dashboard/maquinas";
    }

    @PostMapping("/{id}/excluir")
    public String deletarMaquina(@PathVariable long id, RedirectAttributes attributes, Locale locale) {
        maquinaService.deletarMaquina(id);
        attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_MACHINE_DELETED, null, locale));
        return "redirect:/dashboard/maquinas";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("maquinaDTO") MaquinaDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) return erroValidacao(model, locale);
        maquinaService.editarMaquina(id, dto);
        attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_MACHINE_UPDATED, new Object[]{dto.nome()}, locale));
        return "redirect:/dashboard/maquinas";
    }

    private String erroValidacao(Model model, Locale locale) {
        carregarPagina(model);
        model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        return "maquina/CadastroMaquinas";
    }

    private void carregarPagina(Model model) {
        model.addAttribute("maquinas", maquinaService.listarMaquinas());
        model.addAttribute("setores", setorService.listarSetores());
        if (!model.containsAttribute("maquinaDTO"))
            model.addAttribute("maquinaDTO", new MaquinaDTO(null, null, null, null));
    }
}
