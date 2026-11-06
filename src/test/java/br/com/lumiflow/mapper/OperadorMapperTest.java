package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.entity.Setor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperadorMapperTest {

    private final OperadorMapper mapper = Mappers.getMapper(OperadorMapper.class);

    @Test
    @DisplayName("toEntity copia nome e função e deixa o setor para o service resolver")
    void toEntityIgnoraSetor() {
        Operador operador = mapper.toEntity(new OperadorDTO(4L, "Carlos", "Cortador", 9L));

        assertThat(operador.getId()).isNull(); // o id nunca vem do DTO
        assertThat(operador.getNome()).isEqualTo("Carlos");
        assertThat(operador.getFuncao()).isEqualTo("Cortador");
        assertThat(operador.getSetorPadrao()).isNull();
    }

    @Test
    @DisplayName("toListagemDto expõe id e nome do setor padrão")
    void toListagemComSetor() {
        Setor setor = new Setor();
        setor.setId(9L);
        setor.setNome("DOBRA");
        Operador operador = new Operador();
        operador.setId(4L);
        operador.setNome("CARLOS");
        operador.setFuncao("DOBRADOR");
        operador.setSetorPadrao(setor);

        OperadorListagemDTO dto = mapper.toListagemDto(operador);

        assertThat(dto).isEqualTo(new OperadorListagemDTO(4L, "CARLOS", "DOBRADOR", 9L, "DOBRA"));
    }

    @Test
    @DisplayName("toListagemDto aceita operador sem setor padrão")
    void toListagemSemSetor() {
        Operador operador = new Operador();
        operador.setId(4L);
        operador.setNome("CARLOS");

        OperadorListagemDTO dto = mapper.toListagemDto(operador);

        assertThat(dto.setorPadraoId()).isNull();
        assertThat(dto.setorPadraoNome()).isNull();
    }

    @Test
    @DisplayName("toListDto mapeia a lista e trata nulos")
    void toListDto() {
        Operador a = new Operador();
        a.setNome("A");
        Operador b = new Operador();
        b.setNome("B");

        assertThat(mapper.toListDto(List.of(a, b))).extracting(OperadorListagemDTO::nome).containsExactly("A", "B");
        assertThat(mapper.toListDto(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toListagemDto(null)).isNull();
    }
}
