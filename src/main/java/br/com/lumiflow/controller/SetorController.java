package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
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
        model.addAttribute("setorDTO",new SetorDTO(null,null,null,null));
        return "setor/Setores";
    }

    @PostMapping
    public String novoSetor(@Valid @ModelAttribute("setorDTO") SetorDTO setorDTO, BindingResult result,
                             RedirectAttributes attributes) {
        if (result.hasErrors()) {
            // ---> ADICIONE ESTAS LINHAS AQUI <---
            result.getAllErrors().forEach(error -> {
                System.out.println(">>> ERRO DE VALIDAÇÃO/BIND: " + error.toString());
            });
            // ------------------------------------

            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return  "redirect:/dashboard/setores";
        }
        try {
            setorService.novoSetor(setorDTO);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_SECTOR_CREATED);

        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/setores";

    }

    @PostMapping("{id}/excluir")
    public String excluirSetor(@PathVariable("id") Long id,BindingResult result,RedirectAttributes attributes){
        if (result.hasErrors()) {
            // ---> ADICIONE ESTAS LINHAS AQUI <---
            result.getAllErrors().forEach(error -> {
                System.out.println(">>> ERRO DE VALIDAÇÃO/BIND: " + error.toString());
            });
            // ------------------------------------

            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return  "redirect:/dashboard/setores";
        }
        try {
            setorService.excluirSetor(id);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_SECTOR_DELETED);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard/setores";
    }

    @PostMapping("/{id}/editar")
    public String editarSetor(@PathVariable ("id") Long id,@Valid @ModelAttribute("setorDTO") SetorDTO setorDTO, BindingResult result,
                            RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return  "redirect:/dashboard/setores";
        }
        try {
            setorService.editarSetor(id,setorDTO);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_SECTOR_UPDATED);

        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/setores";

    }

}
