package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.NivelAcessoService;
import br.com.lumiflow.service.SetorService;
import br.com.lumiflow.service.UsuarioService;
import br.com.lumiflow.security.UsuarioDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/dashboard/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final NivelAcessoService nivelAcessoService;
    private final SetorService setorService;
    private final MessageSource messageSource;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listarUsuarios(Model model, Locale locale) {
        try {
            carregarPagina(model);

            return "usuario/ListaUsuario";
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de listagem de usuários: {}", e.getMessage(), e);
            model.addAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            return "usuario/ListaUsuario";
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String novoUsuario(@Valid @ModelAttribute("usuarioDTO") UsuarioDTO dto, BindingResult result,
                              Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, locale);
        }

        try {
            usuarioService.novoUsuario(dto);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_USER_CREATED, new Object[]{dto.login()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao criar usuário '{}': {}", dto.login(), e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("usuarioDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar usuário '{}': {}", dto.login(), e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("usuarioDTO", dto);
        }

        return "redirect:/dashboard/usuario";
    }

    @PostMapping("/{id}/excluir")
    @PreAuthorize("hasRole('ADMIN')")
    public String deletarUsuario(@PathVariable Long id,
                                 @AuthenticationPrincipal UsuarioDetails usuarioLogado,
                                 RedirectAttributes attributes, Locale locale) {
        try {
            usuarioService.excluirUsuario(id, usuarioLogado.getId());
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_USER_DELETED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao excluir usuário id {}: {}", id, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir usuário id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        }

        return "redirect:/dashboard/usuario";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasRole('ADMIN')")
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("usuarioDTO") UsuarioEdicaoDTO dto,
                         BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, locale);
        }

        try {
            usuarioService.editarUsuario(id, dto);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_USER_UPDATED, new Object[]{dto.login()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao editar usuário id {}: {}", id, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("usuarioDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar usuário id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("usuarioDTO", dto);
        }

        return "redirect:/dashboard/usuario";
    }

    private String erroValidacao(Model model, Locale locale) {
        carregarPagina(model);
        model.addAttribute("error", messageSource
                .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        return "usuario/ListaUsuario";
    }

    private void carregarPagina(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("niveisAcesso", nivelAcessoService.listarTodos());
        model.addAttribute("setores", setorService.listarSetores());
        if (!model.containsAttribute("usuarioDTO")) {
            model.addAttribute("usuarioDTO",
                    new UsuarioDTO(null, null, null, null, null, null));
        }
    }
}