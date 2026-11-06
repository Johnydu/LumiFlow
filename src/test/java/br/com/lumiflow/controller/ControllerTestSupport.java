package br.com.lumiflow.controller;

import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.entity.enums.Descricao;
import br.com.lumiflow.security.UsuarioDetails;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Locale;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

final class ControllerTestSupport {

    static final Locale PT_BR = Locale.of("pt", "BR");

    private ControllerTestSupport() {
    }

    /** Usuário simulado com a role informada (sem o prefixo ROLE_); a hierarquia vem da SecurityConfig. */
    static RequestPostProcessor perfil(String role) {
        return user("usuario-" + role.toLowerCase()).roles(role);
    }

    static UsuarioDetails usuarioDetails(Long id, String nome, Descricao nivel) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(nome);
        usuario.setLogin(nome.toLowerCase());
        usuario.setSenha("hash");
        NivelAcesso nivelAcesso = new NivelAcesso();
        nivelAcesso.setDescricao(nivel);
        usuario.setNivelAcesso(nivelAcesso);
        return new UsuarioDetails(usuario);
    }

    /** Principal real (UsuarioDetails), necessário para @AuthenticationPrincipal UsuarioDetails. */
    static RequestPostProcessor logadoComo(Long id, String nome, Descricao nivel) {
        return user(usuarioDetails(id, nome, nivel));
    }
}
