package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.dto.usuario.UsuarioListaDTO;
import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.entity.enums.Descricao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioMapperTest {

    private final UsuarioMapper mapper = Mappers.getMapper(UsuarioMapper.class);

    private Usuario usuario(Setor setor) {
        NivelAcesso nivel = new NivelAcesso();
        nivel.setId(2L);
        nivel.setDescricao(Descricao.SUPERVISOR);
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNome("ANA LIMA");
        u.setLogin("ana.lima");
        u.setSenha("hash");
        u.setNivelAcesso(nivel);
        u.setSetor(setor);
        return u;
    }

    @Test
    @DisplayName("toEntity(UsuarioDTO) nunca copia senha, nível de acesso nem setor (resolvidos no service)")
    void toEntityDtoIgnoraCamposSensiveis() {
        Usuario entity = mapper.toEntity(new UsuarioDTO(1L, "Ana", "ana.lima", "Segredo123", 2L, 5L));

        assertThat(entity.getId()).isNull();
        assertThat(entity.getNome()).isEqualTo("Ana");
        assertThat(entity.getLogin()).isEqualTo("ana.lima");
        assertThat(entity.getSenha()).isNull();
        assertThat(entity.getNivelAcesso()).isNull();
        assertThat(entity.getSetor()).isNull();
    }

    @Test
    @DisplayName("toEntity(UsuarioEdicaoDTO) nunca copia senha, nível de acesso nem setor")
    void toEntityEdicaoIgnoraCamposSensiveis() {
        Usuario entity = mapper.toEntity(new UsuarioEdicaoDTO(1L, "Ana", "ana.lima", 2L, "NovaSenha1", 5L));

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getLogin()).isEqualTo("ana.lima");
        assertThat(entity.getSenha()).isNull();
        assertThat(entity.getNivelAcesso()).isNull();
        assertThat(entity.getSetor()).isNull();
    }

    @Test
    @DisplayName("toUsuarioListaDTO expõe nível (nome do enum), ids e nome do setor")
    void toListaDtoCompleto() {
        Setor setor = new Setor();
        setor.setId(5L);
        setor.setNome("CORTE");

        UsuarioListaDTO dto = mapper.toUsuarioListaDTO(usuario(setor));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.nome()).isEqualTo("ANA LIMA");
        assertThat(dto.login()).isEqualTo("ana.lima");
        assertThat(dto.nivelAcesso()).isEqualTo("SUPERVISOR");
        assertThat(dto.nivelAcessoId()).isEqualTo(2L);
        assertThat(dto.setor()).isEqualTo("CORTE");
        assertThat(dto.setorId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("toUsuarioListaDTO aceita usuário sem setor")
    void toListaDtoSemSetor() {
        UsuarioListaDTO dto = mapper.toUsuarioListaDTO(usuario(null));

        assertThat(dto.setor()).isNull();
        assertThat(dto.setorId()).isNull();
    }

    @Test
    @DisplayName("O DTO de listagem não carrega a senha (não há campo para ela)")
    void listaNaoExpoeSenha() {
        assertThat(UsuarioListaDTO.class.getRecordComponents())
                .extracting(c -> c.getName())
                .doesNotContain("senha");
    }

    @Test
    @DisplayName("toListDTO mapeia lista e trata nulos")
    void toListDto() {
        assertThat(mapper.toListDTO(List.of(usuario(null), usuario(null)))).hasSize(2);
        assertThat(mapper.toListDTO(null)).isNull();
        assertThat(mapper.toUsuarioListaDTO(null)).isNull();
        assertThat(mapper.toEntity((UsuarioDTO) null)).isNull();
        assertThat(mapper.toEntity((UsuarioEdicaoDTO) null)).isNull();
    }
}
