package br.com.lumiflow.controller;

import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.NivelAcessoService;
import br.com.lumiflow.service.SetorService;
import br.com.lumiflow.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final NivelAcessoService nivelAcessoService;
    private final SetorService setorService;

    /**
     * Lista todos os usuários com dados necessários para a página
     */
    @GetMapping
    public String listarUsuarios(Model model) {
        log.info("Requisição GET: Listar todos os usuários");
        
        try {
            model.addAttribute("usuarios", usuarioService.listarTodos());
            model.addAttribute("niveisAcesso", nivelAcessoService.listarTodos());
            model.addAttribute("setores", setorService.listarSetores());
            model.addAttribute("usuarioDTO", new UsuarioDTO(null, null, null, null, null, null));
            
            log.debug("Página de usuários carregada com sucesso");
            return "usuario/ListaUsuario";
            
        } catch (Exception ex) {
            log.error("Erro ao carregar página de usuários: {}", ex.getMessage(), ex);
            model.addAttribute("erro", "Erro ao carregar usuários");
            return "error/500";
        }
    }

    /**
     * Cria novo usuário
     */
    @PostMapping
    public String novoUsuario(@Valid @ModelAttribute("usuarioDTO") UsuarioDTO usuarioDTO,
                              BindingResult result, RedirectAttributes attributes) {
        log.info("Requisição POST: Criar novo usuário. Login: {}", usuarioDTO.login());
        
        if (result.hasErrors()) {
            log.warn("Validação falhou ao criar usuário: {}", usuarioDTO.login());
            attributes.addFlashAttribute("message", "Preencha os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return "redirect:/dashboard/usuario";
        }
        
        try {
            usuarioService.novoUsuario(usuarioDTO);
            attributes.addFlashAttribute("message", "Usuário cadastrado com sucesso");
            attributes.addFlashAttribute("messageType", "success");
            
            log.info("Usuário criado com sucesso via controller: {}", usuarioDTO.login());
            return "redirect:/dashboard/usuario";
            
        } catch (BusinessException ex) {
            log.warn("Erro ao criar usuário: {}. Mensagem: {}", usuarioDTO.login(), ex.getMessage());
            attributes.addFlashAttribute("message", ex.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            log.error("Erro inesperado ao criar usuário: {}. Mensagem: {}", 
                      usuarioDTO.login(), ex.getMessage(), ex);
            attributes.addFlashAttribute("message", "Erro ao criar usuário");
            attributes.addFlashAttribute("messageType", "error");
        }
        
        return "redirect:/dashboard/usuario";
    }

    /**
     * Deleta usuário por ID
     */
    @PostMapping("/{id}/excluir")
    public String deletarUsuario(@PathVariable Long id, RedirectAttributes attributes) {
        log.info("Requisição POST: Deletar usuário. ID: {}", id);
        
        try {
            usuarioService.excluirUsuario(id);
            attributes.addFlashAttribute("message", "Usuário excluído com sucesso");
            attributes.addFlashAttribute("messageType", "success");
            
            log.info("Usuário deletado com sucesso via controller. ID: {}", id);
            
        } catch (BusinessException ex) {
            log.warn("Erro ao deletar usuário ID: {}. Mensagem: {}", id, ex.getMessage());
            attributes.addFlashAttribute("message", ex.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            log.error("Erro inesperado ao deletar usuário ID: {}. Mensagem: {}", 
                      id, ex.getMessage(), ex);
            attributes.addFlashAttribute("message", "Erro ao deletar usuário");
            attributes.addFlashAttribute("messageType", "error");
        }
        
        return "redirect:/dashboard/usuario";
    }

    /**
     * Edita usuário existente
     */
    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @Valid @ModelAttribute("usuarioDTO") UsuarioEdicaoDTO usuarioEdicaoDTO,
                         BindingResult result, RedirectAttributes attributes) {
        log.info("Requisição POST: Editar usuário. ID: {}, Novo Login: {}", id, usuarioEdicaoDTO.login());
        
        if (result.hasErrors()) {
            log.warn("Validação falhou ao editar usuário ID: {}", id);
            attributes.addFlashAttribute("message", "Preencha os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return "redirect:/dashboard/usuario";
        }
        
        try {
            usuarioService.editarUsuario(id, usuarioEdicaoDTO);
            attributes.addFlashAttribute("message", "Usuário atualizado com sucesso");
            attributes.addFlashAttribute("messageType", "success");
            
            log.info("Usuário atualizado com sucesso via controller. ID: {}", id);
            
        } catch (BusinessException ex) {
            log.warn("Erro ao editar usuário ID: {}. Mensagem: {}", id, ex.getMessage());
            attributes.addFlashAttribute("message", ex.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        } catch (Exception ex) {
            log.error("Erro inesperado ao editar usuário ID: {}. Mensagem: {}", 
                      id, ex.getMessage(), ex);
            attributes.addFlashAttribute("message", "Erro ao editar usuário");
            attributes.addFlashAttribute("messageType", "error");
        }
        
        return "redirect:/dashboard/usuario";
    }
}


