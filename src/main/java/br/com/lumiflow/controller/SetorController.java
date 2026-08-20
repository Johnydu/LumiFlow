package br.com.lumiflow.controller;
import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.setor.SetorDTO;
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

@Controller @AllArgsConstructor @RequestMapping("/dashboard/setores")
public class SetorController {
    private final SetorService setorService; private final MessageSource messageSource;
    @GetMapping public String setores(Model model) { carregarPagina(model); return "setor/Setores"; }
    @PostMapping public String novoSetor(@Valid @ModelAttribute("setorDTO") SetorDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) { if (result.hasErrors()) return erroValidacao(model, locale); setorService.novoSetor(dto); attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_SECTOR_CREATED, new Object[]{dto.nome()}, locale)); return "redirect:/dashboard/setores"; }
    @PostMapping("{id}/excluir") public String excluirSetor(@PathVariable Long id, RedirectAttributes attributes, Locale locale) { setorService.excluirSetor(id); attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_SECTOR_DELETED, null, locale)); return "redirect:/dashboard/setores"; }
    @PostMapping("/{id}/editar") public String editarSetor(@PathVariable Long id, @Valid @ModelAttribute("setorDTO") SetorDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) { if (result.hasErrors()) return erroValidacao(model, locale); setorService.editarSetor(id, dto); attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_SECTOR_UPDATED, new Object[]{dto.nome()}, locale)); return "redirect:/dashboard/setores"; }
    private String erroValidacao(Model model, Locale locale) { carregarPagina(model); model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale)); return "setor/Setores"; }
    private void carregarPagina(Model model) { model.addAttribute("setores", setorService.listarSetores()); if (!model.containsAttribute("setorDTO")) model.addAttribute("setorDTO", new SetorDTO(null, null, null, null)); }
}
