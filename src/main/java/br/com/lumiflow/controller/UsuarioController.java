package br.com.lumiflow.controller;

import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
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
                       BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message","Preencha os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return "redirect:/dashboard/usuario";}
        try {
            usuarioService.novoUsuario(usuarioDTO);
            attributes.addFlashAttribute("message", "Usuário cadastrado com sucesso");
            attributes.addFlashAttribute("messageType", "success");

            return "redirect:/dashboard/usuario";
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
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
                setorService.listarSetores()
        );

        model.addAttribute(
                "usuarioDTO",
                new UsuarioDTO(null,null,null,null,null,null)
        );

        return "/usuario/ListaUsuario";
    }

    @PostMapping("/{id}/excluir")
    public String deletarUsuario(@PathVariable Long id, RedirectAttributes attributes ){

        try {
            usuarioService.excluirUsuario(id);attributes.addFlashAttribute("message", "Usuário excluido com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/dashboard/usuario";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute("usuarioDTO")UsuarioEdicaoDTO usuarioEdicaoDTO,
                         BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message", "Preencha os campos corretamente");
            return "redirect:/dashboard/usuario";
        }
        try {
            usuarioService.atualizarUsuario( id, usuarioEdicaoDTO);
            attributes.addFlashAttribute("message", "Usuário atualizado com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/dashboard/usuario";
    }

}


