package br.com.lumiflow.controller;
import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.service.NivelAcessoService;
import br.com.lumiflow.service.SetorService;
import br.com.lumiflow.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Locale;

@Controller @AllArgsConstructor @RequestMapping("/dashboard/usuario")
public class UsuarioController {
    private final UsuarioService usuarioService; private final NivelAcessoService nivelAcessoService; private final SetorService setorService; private final MessageSource messageSource;
    @GetMapping public String listarUsuarios(Model model) { carregarPagina(model); return "usuario/ListaUsuario"; }
    @PostMapping public String novoUsuario(@Valid @ModelAttribute("usuarioDTO") UsuarioDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) { if (result.hasErrors()) return erroValidacao(model, locale); usuarioService.novoUsuario(dto); attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_USER_CREATED, new Object[]{dto.login()}, locale)); return "redirect:/dashboard/usuario"; }
    @PostMapping("/{id}/excluir") public String deletarUsuario(@PathVariable Long id, RedirectAttributes attributes, Locale locale) { usuarioService.excluirUsuario(id); attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_USER_DELETED, null, locale)); return "redirect:/dashboard/usuario"; }
    @PostMapping("/{id}/editar") public String editar(@PathVariable Long id, @Valid @ModelAttribute("usuarioDTO") UsuarioEdicaoDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) { if (result.hasErrors()) return erroValidacao(model, locale); usuarioService.editarUsuario(id, dto); attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_USER_UPDATED, new Object[]{dto.login()}, locale)); return "redirect:/dashboard/usuario"; }
    private String erroValidacao(Model model, Locale locale) { carregarPagina(model); model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale)); return "usuario/ListaUsuario"; }
    private void carregarPagina(Model model) { model.addAttribute("usuarios", usuarioService.listarTodos()); model.addAttribute("niveisAcesso", nivelAcessoService.listarTodos()); model.addAttribute("setores", setorService.listarSetores()); if (!model.containsAttribute("usuarioDTO")) model.addAttribute("usuarioDTO", new UsuarioDTO(null, null, null, null, null, null)); }
}
