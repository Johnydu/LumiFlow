package br.com.lumiflow.security;

import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ============================================================
 * USER DETAILS SERVICE — LumiFlow
 * ============================================================
 * Implementação do UserDetailsService do Spring Security.
 *
 * Quando o usuário tenta fazer login, o Spring Security chama
 * o método loadUserByUsername() passando o valor digitado no
 * campo "login" do formulário.
 *
 * Aqui buscamos o usuário no banco pelo login e retornamos
 * um UsuarioDetails (nosso adaptador) para o Spring Security
 * validar a senha e registrar a sessão.
 * ============================================================
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // ============================================================
    // INJEÇÃO DE DEPENDÊNCIA VIA CONSTRUTOR (SOLID - DIP)
    // ============================================================
    private final UsuarioRepository usuarioRepository;

    public UserDetailsServiceImpl(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // ============================================================
    // CARREGA O USUÁRIO PELO LOGIN
    // Chamado automaticamente pelo Spring Security no login
    // ============================================================
    @Override
    @Transactional(readOnly = true) // evita LazyInitializationException ao acessar setorId
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {

        // Busca o usuário no banco pelo campo 'login'
        Usuario usuario = usuarioRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado: " + login
                ));

        // Retorna o adaptador que o Spring Security entende
        return new UsuarioDetails(usuario);
    }
}