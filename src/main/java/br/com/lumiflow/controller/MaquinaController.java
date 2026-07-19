package br.com.lumiflow.controller;

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
@RequestMapping("/dashboard/maquina")
public class MaquinaController {

    private final SetorService setorService;
    private final MaquinaService maquinaService;

    @GetMapping
    public String maquina(Model model) {
        model.addAttribute("maquinas", maquinaService.listarMaquinas());
        model.addAttribute("maquinaDTO", new MaquinaDTO(null, null, null));
        model.addAttribute("setores", setorService.listarSetores());
        return "maquina/CadastroMaquinas";
    }

    @PostMapping
    public String novaMquina(@Valid @ModelAttribute("maquinaDTO") MaquinaDTO maquinaDTO, BindingResult result,
                             RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message","Preencha todos os campos corretamente");
        }
        try {
            maquinaService.novaMaquina(maquinaDTO);

        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/dashboard/maquina";

    }

    @PostMapping("/{id}")
    public String deletarMaquina(@Valid @PathVariable long id, RedirectAttributes attributes) {

        try {
            maquinaService.deletarMaquina(id);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
        }

        return "redirect:/dashboard/maquina";

    }

}