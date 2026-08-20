package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.SetorService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("/dashboard/operadores")
public class OperadorController {

    private final OperadorService operadorService;
    private final SetorService setorService;

    @GetMapping
    public String listaOperadores(Model model) {

        model.addAttribute("operadores",operadorService.listarOperadores());
        model.addAttribute("listaSetores",setorService.listarSetores());
        model.addAttribute("operadorDTO",new OperadorDTO(null,null,null,null));

        return "operador/Operadores";
    }

    @PostMapping
    public String novoOperador(@Valid @ModelAttribute("operadorDTO")  OperadorDTO operadorDTO, BindingResult result,
                               RedirectAttributes attributes) {
        log.info("Requisição POST: Criar novo operador. Nome: {}", operadorDTO.nome());
        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return "redirect:/dashboard/operadores";
        }

        try {
            operadorService.novoOperador(operadorDTO);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_OPERATOR_CREATED);

        } catch (BusinessException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
            log.error(ex.getMessage());
        }
        return  "redirect:/dashboard/operadores";
    }

    @PostMapping("/{id}/excluir")
    public String excluirOperador(@Valid @PathVariable("id") Long id, RedirectAttributes attributes) {

        try {
            operadorService.excluirOperador(id);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_OPERATOR_DELETED);
        } catch (BusinessException ex) {
            attributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/dashboard/operadores";
    }

    @PostMapping("{id}/editar")
    public String editarOperador(@PathVariable long id, @Valid @ModelAttribute("operadorDTO") OperadorDTO operadorDTO,
                                 BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return  "redirect:/dashboard/operadores";
        }

        try {
            operadorService.editarOperador(id,operadorDTO);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_OPERATOR_UPDATED);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/operadores";
    }

}
