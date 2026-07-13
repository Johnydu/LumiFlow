package br.com.lumiflow.controller;

import br.com.lumiflow.dto.UsuarioDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.NivelAcessoService;
import br.com.lumiflow.service.SetorService;
import br.com.lumiflow.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final NivelAcessoService nivelAcessoService;
    private final SetorService setorService;


    @PostMapping
    public String novo(@Valid @ModelAttribute("usuarioDTO") UsuarioDTO usuarioDTO,
                       BindingResult result, RedirectAttributes attributes, Model model) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message","Preencha os campos corretamente");
            return "redirect:/dashboard/usuario";}
        try {
            usuarioService.novoUsuario(usuarioDTO);

            return "redirect:/dashboard/usuario";
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/dashboard/usuario";
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
                new UsuarioDTO(null,null,null,null,null,null)
        );

        return "/usuario/ListaUsuario";
    }

    @PostMapping("/{id}")
    public String deletarUsuario(@PathVariable Long id, RedirectAttributes attributes ){

        try {
            usuarioService.excluirUsuario(id);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/dashboard/usuario";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute("usuarioDTO") UsuarioDTO usuarioDTO,
                         BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message", "Preencha os campos corretamente");
            return "redirect:/dashboard/usuario";
        }
        try {
            usuarioService.atualizarUsuario(id, usuarioDTO);
            attributes.addFlashAttribute("message", "Usuário atualizado com sucesso");
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/dashboard/usuario";
    }


}


