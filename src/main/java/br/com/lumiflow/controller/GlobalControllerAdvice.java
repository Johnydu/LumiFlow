package br.com.lumiflow.controller;
import br.com.lumiflow.dto.usuario.UsuarioLogadoDTO;
import br.com.lumiflow.security.UsuarioDetails;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute("usuarioLogado")
    public UsuarioLogadoDTO usuarioLogado(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof UsuarioDetails usuario)) {
            return null;
        }

        return new UsuarioLogadoDTO(
                usuario.getNomeCompleto(),
                usuario.getNivelAcesso()
        );
    }
}
