package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.entity.Maquina;
import br.com.lumiflow.entity.Setor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaquinaMapperTest {

    private final MaquinaMapper mapper = Mappers.getMapper(MaquinaMapper.class);

    @Test
    @DisplayName("toEntity normaliza o nome (trim + maiúsculas) e não preenche o setor")
    void toEntityNormalizaNomeEIgnoraSetor() {
        Maquina maquina = mapper.toEntity(new MaquinaDTO(3L, "  serra fita ", "Corte", 7L));

        assertThat(maquina.getId()).isNull(); // o id nunca vem do DTO
        assertThat(maquina.getNome()).isEqualTo("SERRA FITA");
        assertThat(maquina.getSetor()).isNull();
    }

    @Test
    @DisplayName("toEntity mantém nome nulo")
    void toEntityNomeNulo() {
        assertThat(mapper.toEntity(new MaquinaDTO(null, null, null, 1L)).getNome()).isNull();
    }

    @Test
    @DisplayName("toDto expõe nome e id do setor")
    void toDtoMapeiaSetor() {
        Setor setor = new Setor();
        setor.setId(7L);
        setor.setNome("CORTE");
        Maquina maquina = new Maquina();
        maquina.setId(3L);
        maquina.setNome("SERRA");
        maquina.setSetor(setor);

        MaquinaDTO dto = mapper.toDto(maquina);

        assertThat(dto.id()).isEqualTo(3L);
        assertThat(dto.nome()).isEqualTo("SERRA");
        assertThat(dto.setor()).isEqualTo("CORTE");
        assertThat(dto.setorId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("toDto tolera máquina sem setor")
    void toDtoSemSetor() {
        Maquina maquina = new Maquina();
        maquina.setNome("SERRA");

        MaquinaDTO dto = mapper.toDto(maquina);

        assertThat(dto.setor()).isNull();
        assertThat(dto.setorId()).isNull();
    }

    @Test
    @DisplayName("Entradas nulas retornam nulo")
    void entradasNulas() {
        assertThat(mapper.toDto(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDtoList(null)).isNull();
    }

    @Test
    @DisplayName("toDtoList mapeia todos os elementos mantendo a ordem")
    void toDtoListMantemOrdem() {
        Maquina a = new Maquina();
        a.setNome("A");
        Maquina b = new Maquina();
        b.setNome("B");

        List<MaquinaDTO> lista = mapper.toDtoList(List.of(a, b));

        assertThat(lista).extracting(MaquinaDTO::nome).containsExactly("A", "B");
        assertThat(mapper.toDtoList(List.of())).isEmpty();
    }
}
