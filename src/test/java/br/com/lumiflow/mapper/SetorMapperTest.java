package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.entity.Setor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SetorMapperTest {

    private final SetorMapper mapper = Mappers.getMapper(SetorMapper.class);

    private EtapaSetor etapa(Long id, String nome, int ordem) {
        EtapaSetor e = new EtapaSetor();
        e.setId(id);
        e.setNome(nome);
        e.setOrdem(ordem);
        return e;
    }

    @Test
    @DisplayName("toEntity copia id e nome e deixa as etapas para o service")
    void toEntityIgnoraEtapas() {
        SetorDTO dto = new SetorDTO(2L, "Dobra", List.of(new EtapaSetorDTO(null, "1ª", 1)));

        Setor setor = mapper.toEntity(dto);

        assertThat(setor.getId()).isNull(); // o id nunca vem do DTO
        assertThat(setor.getNome()).isEqualTo("Dobra");
        assertThat(setor.getEtapas()).isEmpty();
    }

    @Test
    @DisplayName("toListDTO mapeia etapas e inicia a quantidade de máquinas em zero")
    void toListDto() {
        Setor setor = new Setor();
        setor.setId(2L);
        setor.setNome("DOBRA");
        setor.setEtapas(new ArrayList<>(List.of(etapa(10L, "1ª DOBRA", 1), etapa(11L, "2ª DOBRA", 2))));

        SetorListagemDTO dto = mapper.toListDTO(setor);

        assertThat(dto.id()).isEqualTo(2L);
        assertThat(dto.nome()).isEqualTo("DOBRA");
        assertThat(dto.quantidadeMaquinas()).isZero();
        assertThat(dto.etapas()).containsExactly(
                new EtapaSetorDTO(10L, "1ª DOBRA", 1),
                new EtapaSetorDTO(11L, "2ª DOBRA", 2));
    }

    @Test
    @DisplayName("toEtapaDtoList com lista nula retorna lista vazia")
    void etapasNulas() {
        assertThat(mapper.toEtapaDtoList(null)).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("toListDTO de lista de setores e nulos")
    void listaDeSetores() {
        Setor setor = new Setor();
        setor.setId(1L);
        setor.setNome("A");

        assertThat(mapper.toListDTO(List.of(setor))).hasSize(1);
        assertThat(mapper.toListDTO((Setor) null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
