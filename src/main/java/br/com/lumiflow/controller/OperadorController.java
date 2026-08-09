package br.com.lumiflow.controller;

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
            attributes.addFlashAttribute("message", "Preencha os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return "redirect:/dashboard/operadores";
        }

        try {
            operadorService.novoOperador(operadorDTO);
            attributes.addFlashAttribute("message", "Operador cadastrado com sucesso");
            attributes.addFlashAttribute("messageType", "success");

        } catch (BusinessException ex) {
            attributes.addFlashAttribute("message", ex.getMessage());
            attributes.addFlashAttribute("messageType", "error");
            log.error(ex.getMessage());
        }
        return  "redirect:/dashboard/operadores";
    }

    @PostMapping("/{id}/excluir")
    public String excluirOperador(@Valid @PathVariable("id") Long id, RedirectAttributes attributes) {

        try {
            operadorService.excluirOperador(id);
            attributes.addFlashAttribute("message","Operador excluido com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (BusinessException ex) {
            attributes.addFlashAttribute("message", ex.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/dashboard/operadores";
    }

    @PostMapping("{id}/editar")
    public String editarOperador(@PathVariable long id, @Valid @ModelAttribute("operadorDTO") OperadorDTO operadorDTO,
                                 BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message", "Preencha os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return  "redirect:/dashboard/operadores";
        }

        try {
            operadorService.editarOperador(id,operadorDTO);
            attributes.addFlashAttribute("message", "Operador atualizado com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/dashboard/operadores";
    }

}
