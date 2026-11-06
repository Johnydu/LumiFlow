package br.com.lumiflow.security;

import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.entity.enums.Descricao;
import br.com.lumiflow.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl")
class UserDetailsServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UserDetailsServiceImpl service() {
        return new UserDetailsServiceImpl(usuarioRepository);
    }

    static Usuario usuario(String login, Descricao descricao) {
        Usuario u = new Usuario();
        u.setId(7L);
        u.setNome("MARIA SOUZA");
        u.setLogin(login);
        u.setSenha("$2a$12$hash");
        if (descricao != null) {
            NivelAcesso nivel = new NivelAcesso();
            nivel.setDescricao(descricao);
            u.setNivelAcesso(nivel);
        }
        return u;
    }

    @Test
    @DisplayName("deve carregar usuario existente pelo login")
    void deveCarregarUsuarioExistente() {
        when(usuarioRepository.findByLogin("maria")).thenReturn(Optional.of(usuario("maria", Descricao.PCP)));

        UserDetails details = service().loadUserByUsername("maria");

        assertThat(details.getUsername()).isEqualTo("maria");
        assertThat(details.getPassword()).isEqualTo("$2a$12$hash");
        assertThat(details).isInstanceOf(UsuarioDetails.class);
        verify(usuarioRepository).findByLogin("maria");
    }

    @ParameterizedTest(name = "perfil {0} vira ROLE_{0}")
    @EnumSource(Descricao.class)
    @DisplayName("deve mapear cada perfil para a authority ROLE_<perfil>")
    void deveMapearPerfilParaAuthority(Descricao descricao) {
        when(usuarioRepository.findByLogin("u1")).thenReturn(Optional.of(usuario("u1", descricao)));

        UserDetails details = service().loadUserByUsername("u1");

        assertThat(details.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_" + descricao.name());
    }

    @Test
    @DisplayName("deve lancar UsernameNotFoundException quando login nao existe")
    void deveFalharQuandoNaoExiste() {
        when(usuarioRepository.findByLogin("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().loadUserByUsername("fantasma"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("fantasma");
    }

    @Test
    @DisplayName("deve tratar tentativa de SQL injection como login comum (apenas delega ao repositorio)")
    void deveDelegarLoginMalicioso() {
        String malicioso = "' OR '1'='1";
        when(usuarioRepository.findByLogin(malicioso)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().loadUserByUsername(malicioso))
                .isInstanceOf(UsernameNotFoundException.class);
        verify(usuarioRepository).findByLogin(malicioso);
    }

    @Test
    @DisplayName("deve expor conta bloqueada quando bloqueadoAte esta no futuro")
    void deveExporContaBloqueada() {
        Usuario u = usuario("bloq", Descricao.OPERADOR);
        u.setBloqueadoAte(LocalDateTime.now().plusMinutes(10));
        when(usuarioRepository.findByLogin("bloq")).thenReturn(Optional.of(u));

        UserDetails details = service().loadUserByUsername("bloq");

        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("deve expor conta desbloqueada quando bloqueio expirou")
    void deveExporContaDesbloqueadaAposExpirar() {
        Usuario u = usuario("exp", Descricao.OPERADOR);
        u.setBloqueadoAte(LocalDateTime.now().minusSeconds(1));
        when(usuarioRepository.findByLogin("exp")).thenReturn(Optional.of(u));

        assertThat(service().loadUserByUsername("exp").isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("deve carregar setor do usuario para o UsuarioDetails")
    void deveCarregarSetor() {
        Usuario u = usuario("setor", Descricao.OPERADOR);
        Setor setor = new Setor();
        setor.setId(3L);
        setor.setNome("CORTE");
        u.setSetor(setor);
        when(usuarioRepository.findByLogin("setor")).thenReturn(Optional.of(u));

        UsuarioDetails details = (UsuarioDetails) service().loadUserByUsername("setor");

        assertThat(details.getSetorId()).isEqualTo(3L);
        assertThat(details.getNomeSetor()).isEqualTo("CORTE");
    }
}
