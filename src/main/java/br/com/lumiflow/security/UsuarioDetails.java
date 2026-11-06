package br.com.lumiflow.security;

import br.com.lumiflow.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public class UsuarioDetails implements UserDetails {

    private final Long id;
    private final String nome;
    private final String login;
    private final String senha;
    private final String nivelAcesso;
    private final String nomeSetor;
    private final Long setorId;
    private final LocalDateTime bloqueadoAte;

    public UsuarioDetails(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.login = usuario.getLogin();
        this.senha = usuario.getSenha();
        this.nivelAcesso = usuario.getNivelAcesso() == null
                || usuario.getNivelAcesso().getDescricao() == null
                ? "USER"
                : usuario.getNivelAcesso().getDescricao().name();
        var setor = usuario.getSetor();
        this.nomeSetor = setor == null ? "—" : setor.getNome();
        this.setorId = setor == null ? null : setor.getId();
        this.bloqueadoAte = usuario.getBloqueadoAte();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + nivelAcesso));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return login;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return bloqueadoAte == null || !bloqueadoAte.isAfter(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Long getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nome;
    }

    public String getNomeSetor() {
        return nomeSetor;
    }

    public Long getSetorId() {
        return setorId;
    }

    public String getNivelAcesso() {
        return nivelAcesso;
    }

    public String getIniciais() {
        if (nome == null || nome.isBlank()) {
            return "?";
        }
        String[] partes = nome.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, 1).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }

    // Necessário para o SessionRegistry reconhecer o mesmo usuário em logins diferentes.
    @Override
    public boolean equals(Object outro) {
        return outro instanceof UsuarioDetails usuario && login.equals(usuario.login);
    }

    @Override
    public int hashCode() {
        return login.hashCode();
    }
}
