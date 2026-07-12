package br.com.lumiflow.security;

import br.com.lumiflow.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * ============================================================
 * USUARIO DETAILS — LumiFlow
 * ============================================================
 * Adaptador que converte nossa entidade Usuario para o formato
 * que o Spring Security entende (UserDetails).
 *
 * O Spring Security não conhece nossa classe Usuario — ele só
 * trabalha com UserDetails. Esta classe faz essa ponte.
 *
 * Também expõe dados extras do usuário logado (ex: setorId, nome)
 * para uso no Thymeleaf com sec:authentication.
 * ============================================================
 */
public class UsuarioDetails implements UserDetails {

    // ============================================================
    // ENTIDADE USUARIO ENCAPSULADA
    // Guardamos a entidade completa para acessar dados extras
    // ============================================================
    private final Usuario usuario;

    public UsuarioDetails(Usuario usuario) {
        this.usuario = usuario;
    }

    // ============================================================
    // RETORNA AS PERMISSÕES (ROLES) DO USUÁRIO
    // O Spring Security exige o prefixo ROLE_ nas authorities.
    // Ex: ROLE_SUPORTE, ROLE_GESTAO, ROLE_PCP_SUPERVISOR, ROLE_OPERADOR
    // ============================================================
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority(
                        "ROLE_" + usuario.getNivelAcesso().getDescricao().name()
                )
        );
    }

    // ============================================================
    // SENHA CRIPTOGRAFADA — Spring Security usa para validar o login
    // ============================================================
    @Override
    public String getPassword() {
        return usuario.getSenha();
    }

    // ============================================================
    // USERNAME — usamos o campo 'login' da nossa entidade
    // ============================================================
    @Override
    public String getUsername() {
        return usuario.getLogin();
    }

    // ============================================================
    // ESTADOS DA CONTA — todos true (não implementamos bloqueio)
    // Futuramente: adicionar campo 'ativo' na entidade Usuario
    // ============================================================
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // ============================================================
    // MÉTODOS EXTRAS — acesso aos dados do usuário logado
    // Usados no Thymeleaf: ${#authentication.principal.nomeCompleto}
    // ============================================================

    /**
     * Retorna o nome completo do usuário logado.
     * Uso no HTML: sec:authentication="principal.nomeCompleto"
     */
    public String getNomeCompleto() {
        return usuario.getNome();
    }

    /**
     * Retorna o nome do setorId do usuário logado.
     * Uso no HTML: ${#authentication.principal.nomeSetor}
     */
    public String getNomeSetor() {
        if (usuario.getSetor() == null) return "—";
        return usuario.getSetor().getNome();
    }

    /**
     * Retorna o ID do setorId do usuário logado.
     * Usado no Service para filtrar ordens apenas do setorId do operador.
     */
    public Long getSetorId() {
        if (usuario.getSetor() == null) return null;
        return usuario.getSetor().getId();
    }

    /**
     * Retorna o nível de acesso como string legível.
     * Uso no HTML: ${#authentication.principal.nivelAcessoId}
     */
    public String getNivelAcesso() {
        return usuario.getNivelAcesso().getDescricao().name();
    }

    /**
     * Retorna as iniciais do nome para o avatar no menu.
     * Ex: "Johny Erick" → "JE"
     */
    public String getIniciais() {
        String[] partes = usuario.getNome().trim().split("\\s+");
        if (partes.length == 1) return partes[0].substring(0, 1).toUpperCase();
        return (partes[0].substring(0, 1) + partes[partes.length - 1].substring(0, 1)).toUpperCase();
    }

    /**
     * Retorna a entidade Usuario completa.
     * Use apenas quando precisar de dados que não estão nos métodos acima.
     */
    public Usuario getUsuario() {
        return usuario;
    }
}