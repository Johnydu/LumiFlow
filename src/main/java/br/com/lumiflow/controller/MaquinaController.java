package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.MaquinaService;
import br.com.lumiflow.service.SetorService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/maquinas")
public class MaquinaController {

    private final SetorService setorService;
    private final MaquinaService maquinaService;


    @GetMapping
    public String maquinas(Model model) {
        model.addAttribute("maquinas", maquinaService.listarMaquinas());
        model.addAttribute("maquinaDTO", new MaquinaDTO(null, null, null, null));
        model.addAttribute("setores", setorService.listarSetores());
        return "maquina/CadastroMaquinas";
    }

    @PostMapping
    public String novaMquina(@Valid @ModelAttribute("maquinaDTO") MaquinaDTO maquinaDTO, BindingResult result,
                             RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return  "redirect:/dashboard/maquinas";
        }
        try {
            maquinaService.novaMaquina(maquinaDTO);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_MACHINE_CREATED);

        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/maquinas";

    }

    @PostMapping("/{id}/excluir")
    public String deletarMaquina(@Valid @PathVariable long id, RedirectAttributes attributes) {

        try {
            maquinaService.deletarMaquina(id);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_MACHINE_DELETED);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard/maquinas";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute("maquinaDTO") MaquinaDTO maquinaDTO,
                         BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return "redirect:/dashboard/maquinas";
        }
        try {
            maquinaService.editarMaquina( id, maquinaDTO);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_MACHINE_UPDATED);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/maquinas";
    }

}
