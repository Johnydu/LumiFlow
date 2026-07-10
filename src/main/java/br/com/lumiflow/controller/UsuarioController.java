package br.com.lumiflow.controller;

import br.com.lumiflow.dto.UsuarioDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static org.apache.coyote.http11.Constants.a;


@Controller
@AllArgsConstructor
@RequestMapping("/admin/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;


    @PostMapping
    public String novo(@Valid @ModelAttribute("usuarioDTO") UsuarioDTO usuarioDTO,
                       BindingResult result, RedirectAttributes attributes, Model model) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message","Preencha os campos corretamente");
            return "redirect:/admin/usuario";
        }
        try {
            usuarioService.novoUsuario(usuarioDTO);
            attributes.addFlashAttribute("message", "Usuario novo com sucesso");

            return  "redirect:/admin/usuario";
        } catch (BusinessException e) {
            model.addAttribute("message", e.getMessage());
        }
        return  "redirect:/admin/usuario";
    }

    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
                "usuarios",
                usuarioService.listarTodos()
        );

        model.addAttribute(
                "niveisAcesso",
                nivelAcessoService.listarTodos()
        );

        model.addAttribute(
                "setores",
                setorService.listarTodos()
        );

        model.addAttribute(
                "usuarioDTO",
                new UsuarioDTO()
        );

        return "usuario/cadastroUsuario";
    }

    @PostMapping("/{id}")
    public String deletarUsuario(@PathVariable Long id, RedirectAttributes attributes ){

        try {
            usuarioService.excluirUsuario(id);
            attributes.addFlashAttribute("sucesso", "Usuario deletado com sucesso");
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/Usuario";
    }



}


