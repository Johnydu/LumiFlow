package br.com.lumiflow.controller;

import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.exception.BusinessException;
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
@RequestMapping("/dashboard/setores")
public class SetorController {

    private final SetorService setorService;

    @GetMapping
    public String setores(Model model){
        model.addAttribute("setores",setorService.listarSetores());
        model.addAttribute("setorDTO",new SetorDTO(null,null,null));
        return "setor/Setores";
    }

    @PostMapping
    public String novoSetor(@Valid @ModelAttribute("setorDTO") SetorDTO setorDTO, BindingResult result,
                             RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message","Preencha todos os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return  "redirect:/dashboard/setores";
        }
        try {
            setorService.novoSetor(setorDTO);
            attributes.addFlashAttribute("message","Setor cadastrada com sucesso");
            attributes.addFlashAttribute("messageType", "success");

        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/dashboard/setores";

    }

    @PostMapping("{id}/excluir")
    public String excluirSetor(@PathVariable("id") Long id,RedirectAttributes attributes){
        try {
            setorService.exluirSetor(id);
            attributes.addFlashAttribute("message","Setor removido com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/dashboard/setores";
    }

    @PostMapping("/{id}/editar")
    public String editarSetor(@PathVariable ("id") long id,@Valid @ModelAttribute("setorDTO") SetorDTO setorDTO, BindingResult result,
                            RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message","Preencha todos os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return  "redirect:/dashboard/setores";
        }
        try {
            setorService.editarSetor(id,setorDTO);
            attributes.addFlashAttribute("message","Setor cadastrada com sucesso");
            attributes.addFlashAttribute("messageType", "success");

        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/dashboard/setores";

    }

}
