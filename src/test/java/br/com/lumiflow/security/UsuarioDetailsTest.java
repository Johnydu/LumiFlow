package br.com.lumiflow.security;

import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.entity.enums.Descricao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UsuarioDetails")
class UsuarioDetailsTest {

    private static Usuario base(String login, String nome, Descricao descricao) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNome(nome);
        u.setLogin(login);
        u.setSenha("hash");
        if (descricao != null) {
            NivelAcesso n = new NivelAcesso();
            n.setDescricao(descricao);
            u.setNivelAcesso(n);
        }
        return u;
    }

    @Test
    @DisplayName("sem nivel de acesso deve cair em ROLE_USER (sem privilegios de dominio)")
    void semNivelAcesso() {
        UsuarioDetails d = new UsuarioDetails(base("a", "ANA", null));

        assertThat(d.getNivelAcesso()).isEqualTo("USER");
        assertThat(d.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("nivel de acesso sem descricao deve cair em ROLE_USER")
    void nivelSemDescricao() {
        Usuario u = base("a", "ANA", null);
        u.setNivelAcesso(new NivelAcesso());

        assertThat(new UsuarioDetails(u).getNivelAcesso()).isEqualTo("USER");
    }

    @Test
    @DisplayName("sem setor deve usar travessao e setorId nulo")
    void semSetor() {
        UsuarioDetails d = new UsuarioDetails(base("a", "ANA", Descricao.ADMIN));

        assertThat(d.getNomeSetor()).isEqualTo("—");
        assertThat(d.getSetorId()).isNull();
    }

    @Test
    @DisplayName("sem bloqueio a conta esta destravada")
    void semBloqueio() {
        assertThat(new UsuarioDetails(base("a", "ANA", Descricao.ADMIN)).isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("bloqueio no futuro trava; no passado destrava")
    void bloqueioFuturoEPassado() {
        Usuario futuro = base("a", "ANA", Descricao.ADMIN);
        futuro.setBloqueadoAte(LocalDateTime.now().plusMinutes(1));
        Usuario passado = base("b", "ANA", Descricao.ADMIN);
        passado.setBloqueadoAte(LocalDateTime.now().minusMinutes(1));

        assertThat(new UsuarioDetails(futuro).isAccountNonLocked()).isFalse();
        assertThat(new UsuarioDetails(passado).isAccountNonLocked()).isTrue();
    }

    @Test
    @DisplayName("flags de expiracao e habilitacao sempre verdadeiras")
    void flagsFixas() {
        UsuarioDetails d = new UsuarioDetails(base("a", "ANA", Descricao.OPERADOR));

        assertThat(d.isAccountNonExpired()).isTrue();
        assertThat(d.isCredentialsNonExpired()).isTrue();
        assertThat(d.isEnabled()).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource({
            "maria souza, MS",
            "joao, J",
            "  ana   maria   costa  , AC",
            "pedro de alcantara, PA"
    })
    @DisplayName("deve gerar iniciais em maiusculas (primeiro e ultimo nome)")
    void iniciais(String nome, String esperado) {
        assertThat(new UsuarioDetails(base("a", nome, Descricao.OPERADOR)).getIniciais()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("igualdade e hash baseados apenas no login (SessionRegistry)")
    void igualdadePorLogin() {
        UsuarioDetails a = new UsuarioDetails(base("mesmo", "ANA", Descricao.OPERADOR));
        UsuarioDetails b = new UsuarioDetails(base("mesmo", "OUTRO NOME", Descricao.ADMIN));
        UsuarioDetails c = new UsuarioDetails(base("diferente", "ANA", Descricao.OPERADOR));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
        assertThat(a).isNotEqualTo("mesmo");
        assertThat(a).isNotEqualTo(null);
    }

    @Test
    @DisplayName("deve expor id, nome completo e senha")
    void getters() {
        UsuarioDetails d = new UsuarioDetails(base("a", "ANA LIMA", Descricao.OPERADOR));

        assertThat(d.getId()).isEqualTo(1L);
        assertThat(d.getNomeCompleto()).isEqualTo("ANA LIMA");
        assertThat(d.getPassword()).isEqualTo("hash");
        assertThat(d.getUsername()).isEqualTo("a");
    }
}
