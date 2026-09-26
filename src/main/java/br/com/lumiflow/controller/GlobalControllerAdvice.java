package br.com.lumiflow.controller;

import br.com.lumiflow.dto.usuario.UsuarioLogadoDTO;
import br.com.lumiflow.security.UsuarioDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("usuarioLogado")
    public UsuarioLogadoDTO usuarioLogado(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof UsuarioDetails usuario) {
                return new UsuarioLogadoDTO(
                        usuario.getNomeCompleto(),
                        usuario.getNivelAcesso()
                );
            }
        } catch (Exception e) {
            log.error("Erro inesperado ao recuperar dados do usuário logado no GlobalControllerAdvice: {}", e.getMessage(), e);
        }

        return null;
    }
}